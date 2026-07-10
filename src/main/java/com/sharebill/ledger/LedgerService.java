package com.sharebill.ledger;

import com.sharebill.audit.AuditLogDto;
import com.sharebill.audit.AuditLogRepository;
import com.sharebill.audit.AuditService;
import com.sharebill.common.ConflictException;
import com.sharebill.common.IdGenerator;
import com.sharebill.common.NotFoundException;
import com.sharebill.common.SettlementCalculator;
import com.sharebill.expense.ExpenseDto;
import com.sharebill.expense.ExpenseEntity;
import com.sharebill.expense.ExpenseMapper;
import com.sharebill.expense.ExpenseRepository;
import com.sharebill.expense.ParticipantResolver;
import com.sharebill.settlement.SettlementDto;
import com.sharebill.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {
  private static final String STATUS_UNPAID = "archived_unpaid";
  private static final String STATUS_SETTLED = "settled";

  private final LedgerCycleRepository ledgerCycleRepository;
  private final LedgerCycleMemberRepository ledgerCycleMemberRepository;
  private final ExpenseRepository expenseRepository;
  private final SettlementSnapshotRepository snapshotRepository;
  private final SettlementAdjustmentRepository adjustmentRepository;
  private final PaidSettlementRepository paidSettlementRepository;
  private final AuditLogRepository auditLogRepository;
  private final AuditService auditService;
  private final ExpenseMapper expenseMapper;
  private final UserRepository userRepository;
  private final ParticipantResolver participantResolver;

  public LedgerService(LedgerCycleRepository ledgerCycleRepository, LedgerCycleMemberRepository ledgerCycleMemberRepository,
      ExpenseRepository expenseRepository, SettlementSnapshotRepository snapshotRepository,
      SettlementAdjustmentRepository adjustmentRepository, PaidSettlementRepository paidSettlementRepository,
      AuditLogRepository auditLogRepository, AuditService auditService, ExpenseMapper expenseMapper,
      UserRepository userRepository, ParticipantResolver participantResolver) {
    this.ledgerCycleRepository = ledgerCycleRepository;
    this.ledgerCycleMemberRepository = ledgerCycleMemberRepository;
    this.expenseRepository = expenseRepository;
    this.snapshotRepository = snapshotRepository;
    this.adjustmentRepository = adjustmentRepository;
    this.paidSettlementRepository = paidSettlementRepository;
    this.auditLogRepository = auditLogRepository;
    this.auditService = auditService;
    this.expenseMapper = expenseMapper;
    this.userRepository = userRepository;
    this.participantResolver = participantResolver;
  }

  /**
   * Returns the viewer's "active" cycle — the one shown on their home screen.
   * Every user has exactly one active=true membership row at a time. If the
   * viewer has none yet, or their active cycle has since been settled, a
   * brand new {@code archived_unpaid} cycle is created (owned by the viewer)
   * and swapped in as active — this method never returns a settled cycle.
   */
  @Transactional
  public LedgerCycleEntity ensureActiveCycle(String viewerId) {
    Optional<LedgerCycleMemberEntity> activeMember = ledgerCycleMemberRepository.findByUserIdAndActiveTrue(viewerId);
    if (activeMember.isPresent()) {
      LedgerCycleEntity cycle = ledgerCycleRepository.findById(activeMember.get().getLedgerCycleId())
          .orElseThrow(() -> new NotFoundException("Ledger cycle not found: " + activeMember.get().getLedgerCycleId()));
      if (!STATUS_SETTLED.equals(cycle.getStatus())) {
        return cycle;
      }
      // The viewer's active cycle got settled (auto or manual) since they last
      // looked — roll them over to a fresh cycle rather than showing a
      // read-only settled cycle as "home".
      activeMember.get().setActive(false);
      ledgerCycleMemberRepository.save(activeMember.get());
    }
    return createActiveCycle(viewerId);
  }

  private LedgerCycleEntity createActiveCycle(String ownerUserId) {
    LedgerCycleEntity cycle = new LedgerCycleEntity(
        IdGenerator.next("cycle"), ownerUserId, STATUS_UNPAID, LocalDate.now(), Instant.now());
    LedgerCycleEntity saved = ledgerCycleRepository.saveAndFlush(cycle);
    ledgerCycleMemberRepository.save(new LedgerCycleMemberEntity(saved.getId(), ownerUserId, true, Instant.now()));
    return saved;
  }

  @Transactional(readOnly = true)
  public List<SettlementDto> calculateCycleSettlements(String cycleId) {
    List<ExpenseDto> expenses = expenseRepository.findByLedgerCycleIdAndDeletedAtIsNull(cycleId).stream()
        .map(expenseMapper::toDto)
        .toList();

    Set<String> paidPairKeys = paidSettlementRepository.findByLedgerCycleId(cycleId).stream()
        .map(PaidSettlementEntity::getPairKey)
        .collect(java.util.stream.Collectors.toSet());

    List<SettlementDto> base = SettlementCalculator.calculate(expenses, paidPairKeys);

    Map<String, Long> adjustments = new HashMap<>();
    for (SettlementAdjustmentEntity adjustment : adjustmentRepository.findByLedgerCycleId(cycleId)) {
      adjustments.put(adjustment.getPairKey(), adjustment.getDelta());
    }

    List<SettlementDto> result = new ArrayList<>();
    Set<String> handledKeys = new HashSet<>();

    for (SettlementDto settlement : base) {
      String key = settlement.fromMemberId() + "->" + settlement.toMemberId();
      handledKeys.add(key);
      long adjustment = adjustments.getOrDefault(key, 0L);
      long finalAmount = Math.max(0, settlement.amount() + adjustment);
      if (finalAmount > 0 || settlement.paid()) {
        result.add(new SettlementDto(key, settlement.fromMemberId(), settlement.toMemberId(), finalAmount, settlement.paid()));
      }
    }

    for (Map.Entry<String, Long> entry : adjustments.entrySet()) {
      String key = entry.getKey();
      if (!handledKeys.contains(key) && entry.getValue() > 0) {
        int arrowIndex = key.indexOf("->");
        String from = key.substring(0, arrowIndex);
        String to = key.substring(arrowIndex + 2);
        boolean paid = paidPairKeys.contains(key);
        result.add(new SettlementDto(key, from, to, entry.getValue(), paid));
      }
    }

    return result.stream().filter(s -> s.amount() > 0 || s.paid()).toList();
  }

  @Transactional
  public LedgerCycleDetailDto getCurrentCycleDetail(String viewerId) {
    LedgerCycleEntity cycle = ensureActiveCycle(viewerId);
    return buildDetail(cycle, viewerId);
  }

  @Transactional(readOnly = true)
  public LedgerCycleDetailDto getCycleDetail(String viewerId, String cycleId) {
    LedgerCycleEntity cycle = requireCycle(cycleId, viewerId);
    return buildDetail(cycle, viewerId);
  }

  @Transactional(readOnly = true)
  public List<LedgerCycleDto> listCycles(String viewerId) {
    List<LedgerCycleMemberEntity> memberships = ledgerCycleMemberRepository.findByUserId(viewerId);
    if (memberships.isEmpty()) {
      return List.of();
    }
    Map<String, Boolean> activeByCycleId = new HashMap<>();
    for (LedgerCycleMemberEntity membership : memberships) {
      activeByCycleId.put(membership.getLedgerCycleId(), membership.isActive());
    }

    List<LedgerCycleEntity> cycles = ledgerCycleRepository.findAllById(activeByCycleId.keySet());
    Map<String, ParticipantResolver.Resolved> ownerCache = new HashMap<>();
    String viewerMemberId = "user:" + viewerId;

    return cycles.stream()
        .map(cycle -> {
          boolean active = activeByCycleId.getOrDefault(cycle.getId(), false);
          boolean isOwner = cycle.getOwnerUserId().equals(viewerId);
          ParticipantResolver.Resolved owner = ownerCache.computeIfAbsent(cycle.getOwnerUserId(),
              id -> participantResolver.resolve("user:" + id));

          List<SettlementDto> settlements = STATUS_SETTLED.equals(cycle.getStatus())
              ? snapshotRepository.findByLedgerCycleId(cycle.getId()).stream()
                  .map(s -> new SettlementDto(s.getFromMemberId() + "->" + s.getToMemberId(), s.getFromMemberId(),
                      s.getToMemberId(), s.getAmount(), s.isPaid()))
                  .toList()
              : calculateCycleSettlements(cycle.getId());

          long viewerNet = 0;
          int unpaidCount = 0;
          for (SettlementDto settlement : settlements) {
            if (!settlement.paid()) {
              unpaidCount++;
              if (settlement.toMemberId().equals(viewerMemberId)) viewerNet += settlement.amount();
              if (settlement.fromMemberId().equals(viewerMemberId)) viewerNet -= settlement.amount();
            }
          }

          long totalAmount = expenseRepository.findByLedgerCycleIdAndDeletedAtIsNull(cycle.getId()).stream()
              .mapToLong(ExpenseEntity::getTotalAmount)
              .sum();

          return LedgerCycleDto.withSummary(cycle, active, isOwner, owner.name(), owner.avatarUrl(),
              viewerNet, totalAmount, unpaidCount);
        })
        .sorted(Comparator
            .comparing((LedgerCycleDto dto) -> Boolean.TRUE.equals(dto.active()) ? 0 : 1)
            .thenComparing(LedgerCycleDto::createdAt, Comparator.reverseOrder()))
        .toList();
  }

  @Transactional
  public LedgerCycleDetailDto closeCycle(String actorUserId, String cycleId, String status) {
    LedgerCycleEntity cycle = requireCycle(cycleId, actorUserId);
    if (!STATUS_UNPAID.equals(cycle.getStatus())) {
      throw new ConflictException("Ledger cycle is not open for closing");
    }
    List<SettlementDto> settlements = calculateCycleSettlements(cycle.getId());
    boolean paidFlag = STATUS_SETTLED.equals(status);

    for (SettlementDto settlement : settlements) {
      snapshotRepository.save(new SettlementSnapshotEntity(
          IdGenerator.next("snap"), cycle.getId(), settlement.fromMemberId(), settlement.toMemberId(),
          settlement.amount(), paidFlag));
    }

    LocalDate maxDate = latestBillDate(cycle);

    cycle.setStatus(status);
    cycle.setEndDate(maxDate);
    cycle.setClosedAt(Instant.now());
    cycle.setClosedByUserId(actorUserId);
    ledgerCycleRepository.save(cycle);

    String actorName = actorName(actorUserId);
    String summary = paidFlag ? actorName + " đã tất toán sổ nợ" : actorName + " đã lưu trữ sổ nợ chưa trả";
    String action = paidFlag ? "ledger.settled" : "ledger.archived";
    auditService.log(actorUserId, cycle.getId(), action, "ledger_cycle", cycle.getId(), summary, null,
        LedgerCycleDto.from(cycle));

    return buildDetail(cycle, actorUserId);
  }

  private LocalDate latestBillDate(LedgerCycleEntity cycle) {
    List<ExpenseEntity> cycleExpenses = expenseRepository.findByLedgerCycleIdAndDeletedAtIsNull(cycle.getId());
    LocalDate maxDate = cycle.getStartDate();
    for (ExpenseEntity expense : cycleExpenses) {
      LocalDate billDate = expense.getPaidDate().toLocalDate();
      if (billDate.isAfter(maxDate)) {
        maxDate = billDate;
      }
    }
    LocalDate today = LocalDate.now();
    if (today.isAfter(maxDate)) {
      maxDate = today;
    }
    return maxDate;
  }

  /**
   * Auto-transition: once every settlement pair in an {@code archived_unpaid}
   * cycle has been marked paid (and there's at least one), the cycle
   * transitions to {@code settled} automatically — freezing a snapshot just
   * like a manual "Tất toán". A cycle with zero settlement pairs (e.g. no
   * bills yet, or a single-person cycle) is never auto-settled.
   */
  @Transactional
  public void maybeAutoSettle(LedgerCycleEntity cycle) {
    if (!STATUS_UNPAID.equals(cycle.getStatus())) {
      return;
    }
    List<SettlementDto> settlements = calculateCycleSettlements(cycle.getId());
    if (settlements.isEmpty()) {
      return;
    }
    boolean anyUnpaid = settlements.stream().anyMatch(s -> !s.paid());
    if (anyUnpaid) {
      return;
    }

    for (SettlementDto settlement : settlements) {
      snapshotRepository.save(new SettlementSnapshotEntity(
          IdGenerator.next("snap"), cycle.getId(), settlement.fromMemberId(), settlement.toMemberId(),
          settlement.amount(), true));
    }

    LocalDate maxDate = latestBillDate(cycle);
    cycle.setStatus(STATUS_SETTLED);
    cycle.setEndDate(maxDate);
    cycle.setClosedAt(Instant.now());
    ledgerCycleRepository.save(cycle);

    auditService.log(cycle.getOwnerUserId(), cycle.getId(), "ledger.settled", "ledger_cycle", cycle.getId(),
        "Tự động tất toán vì tất cả khoản nợ đã được trả", null, LedgerCycleDto.from(cycle));
  }

  @Transactional
  public LedgerCycleDetailDto reopenCycle(String actorUserId, String cycleId) {
    LedgerCycleEntity cycle = requireCycle(cycleId, actorUserId);
    if (!STATUS_SETTLED.equals(cycle.getStatus())) {
      throw new ConflictException("Ledger cycle is not settled");
    }

    cycle.setStatus(STATUS_UNPAID);
    cycle.setEndDate(null);
    cycle.setClosedAt(null);
    cycle.setClosedByUserId(null);
    ledgerCycleRepository.save(cycle);

    snapshotRepository.deleteAll(snapshotRepository.findByLedgerCycleId(cycleId));
    paidSettlementRepository.deleteAll(paidSettlementRepository.findByLedgerCycleId(cycleId));

    String actorName = actorName(actorUserId);
    auditService.log(actorUserId, cycle.getId(), "ledger.reopened", "ledger_cycle", cycle.getId(),
        actorName + " đã hủy tất toán sổ nợ", null, LedgerCycleDto.from(cycle));

    return buildDetail(cycle, actorUserId);
  }

  /** "Đưa lên trang chủ": activates {@code cycleId} for {@code viewerId}, swapping off whichever cycle was active. */
  @Transactional
  public LedgerCycleDetailDto setActive(String viewerId, String cycleId) {
    requireCycle(cycleId, viewerId);

    ledgerCycleMemberRepository.findByUserIdAndActiveTrue(viewerId).ifPresent(current -> {
      if (!current.getLedgerCycleId().equals(cycleId)) {
        current.setActive(false);
        ledgerCycleMemberRepository.save(current);
      }
    });

    LedgerCycleMemberEntity member = ledgerCycleMemberRepository.findByLedgerCycleIdAndUserId(cycleId, viewerId)
        .orElseThrow(() -> new NotFoundException("Ledger cycle not found: " + cycleId));
    member.setActive(true);
    ledgerCycleMemberRepository.save(member);

    return getCycleDetail(viewerId, cycleId);
  }

  @Transactional
  public List<SettlementDto> markPaid(String actorUserId, String cycleId, String settlementId) {
    LedgerCycleEntity cycle = requireCycle(cycleId, actorUserId);
    if (!STATUS_UNPAID.equals(cycle.getStatus())) {
      throw new ConflictException("Cannot modify settlements in a settled cycle");
    }

    var existing = paidSettlementRepository.findByLedgerCycleIdAndPairKey(cycleId, settlementId);
    boolean nowPaid;
    if (existing.isPresent()) {
      paidSettlementRepository.delete(existing.get());
      nowPaid = false;
    } else {
      paidSettlementRepository.save(new PaidSettlementEntity(cycleId, settlementId));
      nowPaid = true;
    }

    String actorName = actorName(actorUserId);
    String summary = nowPaid ? actorName + " đã đánh dấu khoản nợ là đã trả" : actorName + " đã hoàn tác khoản nợ thành chưa trả";
    String action = nowPaid ? "settlement.marked_paid" : "settlement.marked_unpaid";
    auditService.log(actorUserId, cycleId, action, "settlement", settlementId, summary, null, null);

    if (nowPaid) {
      maybeAutoSettle(cycle);
    }

    return calculateCycleSettlements(cycleId);
  }

  @Transactional
  public List<SettlementDto> adjustSettlement(String actorUserId, String cycleId, String settlementId, long deltaAmount) {
    LedgerCycleEntity cycle = requireCycle(cycleId, actorUserId);
    if (!STATUS_UNPAID.equals(cycle.getStatus())) {
      throw new ConflictException("Cannot modify settlements in a settled cycle");
    }

    var existing = adjustmentRepository.findByLedgerCycleIdAndPairKey(cycleId, settlementId);
    long previousDelta = existing.map(SettlementAdjustmentEntity::getDelta).orElse(0L);
    long nextDelta = previousDelta + deltaAmount;

    if (existing.isPresent()) {
      existing.get().setDelta(nextDelta);
      adjustmentRepository.save(existing.get());
    } else {
      adjustmentRepository.save(new SettlementAdjustmentEntity(cycleId, settlementId, nextDelta));
    }

    String sign = deltaAmount > 0 ? "+" : "";
    String actorName = actorName(actorUserId);
    auditService.log(actorUserId, cycleId, "settlement.adjusted", "settlement", settlementId,
        actorName + " đã điều chỉnh nợ " + sign + deltaAmount + "đ",
        Map.of("delta", previousDelta), Map.of("delta", nextDelta));

    return calculateCycleSettlements(cycleId);
  }

  private LedgerCycleDetailDto buildDetail(LedgerCycleEntity cycle, String viewerId) {
    List<ExpenseEntity> expenseEntities = expenseRepository.findByLedgerCycleIdAndDeletedAtIsNull(cycle.getId());
    List<ExpenseDto> expenses = expenseEntities.stream()
        .map(expenseMapper::toDto)
        .toList();

    List<SettlementSnapshotDto> settlements;
    if (STATUS_UNPAID.equals(cycle.getStatus())) {
      settlements = calculateCycleSettlements(cycle.getId()).stream()
          .map(s -> new SettlementSnapshotDto(s.id(), cycle.getId(), s.fromMemberId(), s.toMemberId(), s.amount(), s.paid()))
          .toList();
    } else {
      settlements = snapshotRepository.findByLedgerCycleId(cycle.getId()).stream()
          .map(SettlementSnapshotDto::from)
          .toList();
    }

    List<AuditLogDto> auditLogs = auditLogRepository.findByLedgerCycleIdOrderByCreatedAtAsc(cycle.getId()).stream()
        .map(AuditLogDto::from)
        .toList();

    Map<String, LedgerMemberInfoDto> members = buildMembersMap(cycle, expenseEntities, auditLogs);

    boolean active = ledgerCycleMemberRepository.findByLedgerCycleIdAndUserId(cycle.getId(), viewerId)
        .map(LedgerCycleMemberEntity::isActive)
        .orElse(false);
    boolean isOwner = cycle.getOwnerUserId().equals(viewerId);
    ParticipantResolver.Resolved owner = participantResolver.resolve("user:" + cycle.getOwnerUserId());
    LedgerCycleDto cycleDto = LedgerCycleDto.from(cycle, active, isOwner, owner.name(), owner.avatarUrl());

    return new LedgerCycleDetailDto(cycleDto, expenses, settlements, auditLogs, members);
  }

  private Map<String, LedgerMemberInfoDto> buildMembersMap(LedgerCycleEntity cycle, List<ExpenseEntity> expenses,
      List<AuditLogDto> auditLogs) {
    Set<String> memberIds = new LinkedHashSet<>();
    memberIds.add("user:" + cycle.getOwnerUserId());
    for (ExpenseEntity expense : expenses) {
      expense.getPayers().forEach(p -> memberIds.add(p.getMemberId()));
      expense.getParticipants().forEach(p -> memberIds.add(p.getMemberId()));
    }
    for (AuditLogDto log : auditLogs) {
      if (log.ownerUserId() != null) {
        memberIds.add("user:" + log.ownerUserId());
      }
    }

    Map<String, LedgerMemberInfoDto> members = new LinkedHashMap<>();
    for (String memberId : memberIds) {
      ParticipantResolver.Resolved resolved = participantResolver.resolve(memberId);
      boolean isUser = memberId.startsWith("user:");
      members.put(memberId, new LedgerMemberInfoDto(memberId, resolved.name(), resolved.avatarUrl(), isUser));
    }
    return members;
  }

  private String actorName(String userId) {
    return userRepository.findById(userId).map(u -> u.getDisplayName()).orElse(userId);
  }

  private LedgerCycleEntity requireCycle(String cycleId, String actorUserId) {
    LedgerCycleEntity cycle = ledgerCycleRepository.findById(cycleId)
        .orElseThrow(() -> new NotFoundException("Ledger cycle not found: " + cycleId));
    if (!ledgerCycleMemberRepository.existsByLedgerCycleIdAndUserId(cycleId, actorUserId)) {
      throw new NotFoundException("Ledger cycle not found: " + cycleId);
    }
    return cycle;
  }
}
