package com.sharebill.expense;

import com.sharebill.audit.AuditService;
import com.sharebill.common.ConflictException;
import com.sharebill.common.IdGenerator;
import com.sharebill.common.NotFoundException;
import com.sharebill.ledger.LedgerCycleEntity;
import com.sharebill.ledger.LedgerCycleRepository;
import com.sharebill.ledger.LedgerService;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {
  private final ExpenseRepository expenseRepository;
  private final LedgerService ledgerService;
  private final LedgerCycleRepository ledgerCycleRepository;
  private final AuditService auditService;

  public ExpenseService(ExpenseRepository expenseRepository, LedgerService ledgerService,
      LedgerCycleRepository ledgerCycleRepository, AuditService auditService) {
    this.expenseRepository = expenseRepository;
    this.ledgerService = ledgerService;
    this.ledgerCycleRepository = ledgerCycleRepository;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public List<ExpenseDto> listExpenses(String groupId) {
    return expenseRepository.findByGroupIdOrderByPaidDateDesc(groupId).stream()
        .map(ExpenseMapper::toDto)
        .toList();
  }

  @Transactional
  public ExpenseDto createExpense(String groupId, ExpenseDto request, String actorMemberId) {
    validateExpense(groupId, request);
    LedgerCycleEntity cycle = ledgerService.ensureOpenCycle(groupId);

    Instant now = Instant.now();
    ExpenseEntity entity = new ExpenseEntity(request.id(), groupId, cycle.getId(), request.title(),
        request.totalAmount(), request.paidDate(), request.imageUrl(), request.splitMode(), now, now);

    populateLines(entity, request);
    expenseRepository.save(entity);

    ExpenseDto dto = ExpenseMapper.toDto(entity);
    auditService.log(groupId, cycle.getId(), actorMemberId, "expense.created", "expense", entity.getId(),
        "Đã tạo bill " + request.title() + " " + request.totalAmount() + "đ", null, dto);

    return dto;
  }

  @Transactional
  public ExpenseDto updateExpense(String groupId, String expenseId, ExpenseDto request, String actorMemberId) {
    ExpenseEntity entity = requireExpense(groupId, expenseId);
    LedgerCycleEntity cycle = requireOpenCycleForExpense(entity);

    validateExpense(groupId, request);
    ExpenseDto before = ExpenseMapper.toDto(entity);

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
    ExpenseDto after = ExpenseMapper.toDto(entity);

    auditService.log(groupId, cycle.getId(), actorMemberId, "expense.updated", "expense", entity.getId(),
        "Đã sửa bill " + request.title(), before, after);

    return after;
  }

  @Transactional
  public void deleteExpense(String groupId, String expenseId, String actorMemberId) {
    ExpenseEntity entity = requireExpense(groupId, expenseId);
    LedgerCycleEntity cycle = requireOpenCycleForExpense(entity);

    ExpenseDto before = ExpenseMapper.toDto(entity);
    expenseRepository.delete(entity);

    auditService.log(groupId, cycle.getId(), actorMemberId, "expense.deleted", "expense", expenseId,
        "Đã xóa bill " + before.title(), before, null);
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
          participant.memberName(), position++));
    }
  }

  private ExpenseEntity requireExpense(String groupId, String expenseId) {
    ExpenseEntity entity = expenseRepository.findById(expenseId)
        .orElseThrow(() -> new NotFoundException("Expense not found: " + expenseId));
    if (!entity.getGroupId().equals(groupId)) {
      throw new NotFoundException("Expense not found: " + expenseId);
    }
    return entity;
  }

  private LedgerCycleEntity requireOpenCycleForExpense(ExpenseEntity entity) {
    LedgerCycleEntity cycle = ledgerCycleRepository.findById(entity.getLedgerCycleId())
        .orElseThrow(() -> new NotFoundException("Ledger cycle not found: " + entity.getLedgerCycleId()));
    if (!"open".equals(cycle.getStatus())) {
      throw new ConflictException("Cannot modify an expense in a closed ledger cycle");
    }
    return cycle;
  }

  private void validateExpense(String groupId, ExpenseDto expense) {
    if (!groupId.equals(expense.groupId())) {
      throw new IllegalArgumentException("Expense groupId must match path groupId");
    }

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
