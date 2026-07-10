package com.sharebill.expense;

import com.sharebill.audit.AuditService;
import com.sharebill.common.ConflictException;
import com.sharebill.common.NotFoundException;
import com.sharebill.ledger.LedgerCycleEntity;
import com.sharebill.ledger.LedgerCycleMemberEntity;
import com.sharebill.ledger.LedgerCycleMemberRepository;
import com.sharebill.ledger.LedgerCycleRepository;
import com.sharebill.ledger.LedgerService;
import com.sharebill.user.UserRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {
  private static final String STATUS_UNPAID = "archived_unpaid";

  private final ExpenseRepository expenseRepository;
  private final LedgerService ledgerService;
  private final LedgerCycleRepository ledgerCycleRepository;
  private final LedgerCycleMemberRepository ledgerCycleMemberRepository;
  private final AuditService auditService;
  private final ExpenseMapper expenseMapper;
  private final UserRepository userRepository;

  public ExpenseService(ExpenseRepository expenseRepository, LedgerService ledgerService,
      LedgerCycleRepository ledgerCycleRepository, LedgerCycleMemberRepository ledgerCycleMemberRepository,
      AuditService auditService, ExpenseMapper expenseMapper, UserRepository userRepository) {
    this.expenseRepository = expenseRepository;
    this.ledgerService = ledgerService;
    this.ledgerCycleRepository = ledgerCycleRepository;
    this.ledgerCycleMemberRepository = ledgerCycleMemberRepository;
    this.auditService = auditService;
    this.expenseMapper = expenseMapper;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<ExpenseDto> listExpenses(String ownerUserId) {
    return expenseRepository.findByOwnerUserIdAndDeletedAtIsNullOrderByPaidDateDesc(ownerUserId).stream()
        .map(expenseMapper::toDto)
        .toList();
  }

  @Transactional
  public ExpenseDto createExpense(String actorUserId, ExpenseDto request) {
    validateExpense(request);
    LedgerCycleEntity cycle = resolveCreateCycle(actorUserId, request.ledgerCycleId());

    Instant now = Instant.now();
    ExpenseEntity entity = new ExpenseEntity(request.id(), actorUserId, cycle.getId(), request.title(),
        request.totalAmount(), request.paidDate(), request.imageUrl(), request.splitMode(), now, now);

    populateLines(entity, request);
    expenseRepository.save(entity);
    syncCycleMembers(cycle);

    ExpenseDto dto = expenseMapper.toDto(entity);
    String actorName = actorName(actorUserId);
    auditService.log(actorUserId, cycle.getId(), "expense.created", "expense", entity.getId(),
        actorName + " đã tạo bill " + request.title() + " " + request.totalAmount() + "đ", null, dto);

    return dto;
  }

  /**
   * Picks the ledger cycle a newly-created bill belongs to: the caller can
   * target a specific (still chưa-trả) cycle explicitly — used by "+ Thêm
   * bill" from a Sổ nợ detail view — otherwise it falls back to the actor's
   * home-screen (active) cycle. {@link LedgerService#ensureActiveCycle}
   * always returns an {@code archived_unpaid} cycle, so the fallback never
   * needs a separate "was it settled?" check.
   */
  private LedgerCycleEntity resolveCreateCycle(String actorUserId, String requestedCycleId) {
    if (requestedCycleId != null && !requestedCycleId.isBlank()) {
      LedgerCycleEntity requested = ledgerCycleRepository.findById(requestedCycleId).orElse(null);
      if (requested != null && STATUS_UNPAID.equals(requested.getStatus())
          && ledgerCycleMemberRepository.existsByLedgerCycleIdAndUserId(requestedCycleId, actorUserId)) {
        return requested;
      }
    }
    return ledgerService.ensureActiveCycle(actorUserId);
  }

  @Transactional
  public ExpenseDto updateExpense(String actorUserId, String expenseId, ExpenseDto request) {
    ExpenseEntity entity = requireExpense(actorUserId, expenseId);
    LedgerCycleEntity cycle = requireEditableCycle(entity);

    validateExpense(request);
    ExpenseDto before = expenseMapper.toDto(entity);

    entity.setTitle(request.title());
    entity.setTotalAmount(request.totalAmount());
    entity.setPaidDate(request.paidDate());
    entity.setImageUrl(request.imageUrl());
    entity.setSplitMode(request.splitMode());
    entity.setUpdatedAt(Instant.now());
    entity.getPayers().clear();
    entity.getParticipants().clear();
    populateLines(entity, request);

    expenseRepository.save(entity);
    syncCycleMembers(cycle);
    ExpenseDto after = expenseMapper.toDto(entity);

    String actorName = actorName(actorUserId);
    String summary = actorName + " đã sửa bill " + describeChanges(before, after, request.title());
    auditService.log(actorUserId, cycle.getId(), "expense.updated", "expense", entity.getId(),
        summary, before, after);

    return after;
  }

  /** Builds a human-readable "field: before → after" summary for the audit log. */
  private String describeChanges(ExpenseDto before, ExpenseDto after, String title) {
    List<String> changes = new java.util.ArrayList<>();
    if (!before.title().equals(after.title())) {
      changes.add("tên " + before.title() + " → " + after.title());
    }
    if (before.totalAmount() != after.totalAmount()) {
      changes.add("tổng tiền " + before.totalAmount() + "đ → " + after.totalAmount() + "đ");
    }
    if (!java.util.Objects.equals(before.paidDate(), after.paidDate())) {
      changes.add("ngày " + before.paidDate() + " → " + after.paidDate());
    }
    if (!before.splitMode().equals(after.splitMode())) {
      changes.add("cách chia " + before.splitMode() + " → " + after.splitMode());
    }
    if (changes.isEmpty()) {
      return title;
    }
    return title + ": " + String.join(", ", changes);
  }

  @Transactional
  public void deleteExpense(String actorUserId, String expenseId) {
    ExpenseEntity entity = requireExpense(actorUserId, expenseId);
    LedgerCycleEntity cycle = ledgerCycleRepository.findById(entity.getLedgerCycleId())
        .orElseThrow(() -> new NotFoundException("Ledger cycle not found: " + entity.getLedgerCycleId()));

    ExpenseDto before = expenseMapper.toDto(entity);
    entity.setDeletedAt(Instant.now());
    entity.setDeletedByUserId(actorUserId);
    expenseRepository.save(entity);
    syncCycleMembers(cycle);

    String actorName = actorName(actorUserId);
    auditService.log(actorUserId, cycle.getId(), "expense.deleted", "expense", expenseId,
        actorName + " đã xóa bill " + before.title() + " (" + before.totalAmount() + "đ)", before, null);

    // Soft-delete is allowed at any status; only chase the auto-settle
    // transition when the cycle is still open for edits.
    ledgerService.maybeAutoSettle(cycle);
  }

  /**
   * Recomputes cycle membership from the owner plus every {@code user:<id>}
   * payer/participant across the cycle's non-deleted bills. Inserts
   * newly-seen members with {@code active=false} (joining a shared cycle
   * never silently steals someone's home screen — only
   * {@link LedgerService#ensureActiveCycle}/{@code setActive} may flip that
   * flag) and removes members who are no longer referenced by any active
   * bill — except the owner, who is always a member. Existing rows are left
   * untouched so a user's active state survives.
   */
  private void syncCycleMembers(LedgerCycleEntity cycle) {
    Set<String> desiredUserIds = new HashSet<>();
    desiredUserIds.add(cycle.getOwnerUserId());

    for (ExpenseEntity expense : expenseRepository.findByLedgerCycleIdAndDeletedAtIsNull(cycle.getId())) {
      collectUserIds(expense, desiredUserIds);
    }

    List<LedgerCycleMemberEntity> existing = ledgerCycleMemberRepository.findByLedgerCycleId(cycle.getId());
    Set<String> existingUserIds = new HashSet<>();
    for (LedgerCycleMemberEntity member : existing) {
      existingUserIds.add(member.getUserId());
    }

    for (String userId : desiredUserIds) {
      if (!existingUserIds.contains(userId)) {
        ledgerCycleMemberRepository.save(new LedgerCycleMemberEntity(cycle.getId(), userId, false, Instant.now()));
      }
    }

    for (LedgerCycleMemberEntity member : existing) {
      if (!member.getUserId().equals(cycle.getOwnerUserId()) && !desiredUserIds.contains(member.getUserId())) {
        ledgerCycleMemberRepository.delete(member);
      }
    }
  }

  private void collectUserIds(ExpenseEntity expense, Set<String> target) {
    for (ExpensePayerEntity payer : expense.getPayers()) {
      addIfUser(payer.getMemberId(), target);
    }
    for (ExpenseParticipantEntity participant : expense.getParticipants()) {
      addIfUser(participant.getMemberId(), target);
    }
  }

  private void addIfUser(String memberId, Set<String> target) {
    if (memberId != null && memberId.startsWith("user:")) {
      target.add(memberId.substring("user:".length()));
    }
  }

  private void populateLines(ExpenseEntity entity, ExpenseDto request) {
    int position = 0;
    for (PayerContributionDto payer : request.payers()) {
      entity.getPayers().add(new ExpensePayerEntity(entity, payer.memberId(), payer.amount(), position++));
    }
    position = 0;
    for (ParticipantShareDto participant : request.participants()) {
      entity.getParticipants().add(new ExpenseParticipantEntity(
          entity, participant.memberId(), participant.amount(), participant.isCustom(),
          null, position++));
    }
  }

  private ExpenseEntity requireExpense(String actorUserId, String expenseId) {
    ExpenseEntity entity = expenseRepository.findById(expenseId)
        .filter(e -> e.getDeletedAt() == null)
        .orElseThrow(() -> new NotFoundException("Expense not found: " + expenseId));
    if (!ledgerCycleMemberRepository.existsByLedgerCycleIdAndUserId(entity.getLedgerCycleId(), actorUserId)) {
      throw new NotFoundException("Expense not found: " + expenseId);
    }
    return entity;
  }

  private LedgerCycleEntity requireEditableCycle(ExpenseEntity entity) {
    LedgerCycleEntity cycle = ledgerCycleRepository.findById(entity.getLedgerCycleId())
        .orElseThrow(() -> new NotFoundException("Ledger cycle not found: " + entity.getLedgerCycleId()));
    if (!STATUS_UNPAID.equals(cycle.getStatus())) {
      throw new ConflictException("Cannot modify an expense in a settled ledger cycle");
    }
    return cycle;
  }

  private String actorName(String userId) {
    return userRepository.findById(userId).map(u -> u.getDisplayName()).orElse(userId);
  }

  private void validateExpense(ExpenseDto expense) {
    long payerTotal = expense.payers().stream().mapToLong(PayerContributionDto::amount).sum();
    long shareTotal = expense.participants().stream().mapToLong(ParticipantShareDto::amount).sum();

    if (payerTotal != expense.totalAmount()) {
      throw new IllegalArgumentException("Payer total must equal expense total");
    }
    if (shareTotal != expense.totalAmount()) {
      throw new IllegalArgumentException("Participant share total must equal expense total");
    }
  }
}
