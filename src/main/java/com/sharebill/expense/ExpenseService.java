package com.sharebill.expense;

import com.sharebill.audit.AuditService;
import com.sharebill.common.ConflictException;
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
  private final ExpenseMapper expenseMapper;

  public ExpenseService(ExpenseRepository expenseRepository, LedgerService ledgerService,
      LedgerCycleRepository ledgerCycleRepository, AuditService auditService, ExpenseMapper expenseMapper) {
    this.expenseRepository = expenseRepository;
    this.ledgerService = ledgerService;
    this.ledgerCycleRepository = ledgerCycleRepository;
    this.auditService = auditService;
    this.expenseMapper = expenseMapper;
  }

  @Transactional(readOnly = true)
  public List<ExpenseDto> listExpenses(String ownerUserId) {
    return expenseRepository.findByOwnerUserIdOrderByPaidDateDesc(ownerUserId).stream()
        .map(expenseMapper::toDto)
        .toList();
  }

  @Transactional
  public ExpenseDto createExpense(String ownerUserId, ExpenseDto request) {
    validateExpense(request);
    LedgerCycleEntity cycle = ledgerService.ensureOpenCycle(ownerUserId);

    Instant now = Instant.now();
    ExpenseEntity entity = new ExpenseEntity(request.id(), ownerUserId, cycle.getId(), request.title(),
        request.totalAmount(), request.paidDate(), request.imageUrl(), request.splitMode(), now, now);

    populateLines(entity, request);
    expenseRepository.save(entity);

    ExpenseDto dto = expenseMapper.toDto(entity);
    auditService.log(ownerUserId, cycle.getId(), "expense.created", "expense", entity.getId(),
        "Đã tạo bill " + request.title() + " " + request.totalAmount() + "đ", null, dto);

    return dto;
  }

  @Transactional
  public ExpenseDto updateExpense(String ownerUserId, String expenseId, ExpenseDto request) {
    ExpenseEntity entity = requireExpense(ownerUserId, expenseId);
    LedgerCycleEntity cycle = requireOpenCycleForExpense(entity);

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
    ExpenseDto after = expenseMapper.toDto(entity);

    auditService.log(ownerUserId, cycle.getId(), "expense.updated", "expense", entity.getId(),
        "Đã sửa bill " + request.title(), before, after);

    return after;
  }

  @Transactional
  public void deleteExpense(String ownerUserId, String expenseId) {
    ExpenseEntity entity = requireExpense(ownerUserId, expenseId);
    LedgerCycleEntity cycle = requireOpenCycleForExpense(entity);

    ExpenseDto before = expenseMapper.toDto(entity);
    expenseRepository.delete(entity);

    auditService.log(ownerUserId, cycle.getId(), "expense.deleted", "expense", expenseId,
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
          null, position++));
    }
  }

  private ExpenseEntity requireExpense(String ownerUserId, String expenseId) {
    ExpenseEntity entity = expenseRepository.findById(expenseId)
        .orElseThrow(() -> new NotFoundException("Expense not found: " + expenseId));
    if (!entity.getOwnerUserId().equals(ownerUserId)) {
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
