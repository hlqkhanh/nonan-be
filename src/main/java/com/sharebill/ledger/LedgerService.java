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
import com.sharebill.settlement.SettlementDto;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {
  private final LedgerCycleRepository ledgerCycleRepository;
  private final ExpenseRepository expenseRepository;
  private final SettlementSnapshotRepository snapshotRepository;
  private final SettlementAdjustmentRepository adjustmentRepository;
  private final PaidSettlementRepository paidSettlementRepository;
  private final AuditLogRepository auditLogRepository;
  private final AuditService auditService;

  public LedgerService(LedgerCycleRepository ledgerCycleRepository, ExpenseRepository expenseRepository,
      SettlementSnapshotRepository snapshotRepository, SettlementAdjustmentRepository adjustmentRepository,
      PaidSettlementRepository paidSettlementRepository, AuditLogRepository auditLogRepository,
      AuditService auditService) {
    this.ledgerCycleRepository = ledgerCycleRepository;
    this.expenseRepository = expenseRepository;
    this.snapshotRepository = snapshotRepository;
    this.adjustmentRepository = adjustmentRepository;
    this.paidSettlementRepository = paidSettlementRepository;
    this.auditLogRepository = auditLogRepository;
    this.auditService = auditService;
  }

  @Transactional
  public LedgerCycleEntity ensureOpenCycle(String groupId) {
    return ledgerCycleRepository.findByGroupIdAndStatus(groupId, "open")
        .orElseGet(() -> createOpenCycle(groupId));
  }

  private LedgerCycleEntity createOpenCycle(String groupId) {
    try {
      LedgerCycleEntity cycle = new LedgerCycleEntity(
          IdGenerator.next("cycle"), groupId, "open", LocalDate.now(), Instant.now());
      return ledgerCycleRepository.saveAndFlush(cycle);
    } catch (DataIntegrityViolationException e) {
      return ledgerCycleRepository.findByGroupIdAndStatus(groupId, "open").orElseThrow();
    }
  }

  @Transactional(readOnly = true)
  public List<SettlementDto> calculateCycleSettlements(String cycleId) {
    List<ExpenseDto> expenses = expenseRepository.findByLedgerCycleId(cycleId).stream()
        .map(ExpenseMapper::toDto)
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
  public LedgerCycleDetailDto getCurrentCycleDetail(String groupId) {
    LedgerCycleEntity cycle = ensureOpenCycle(groupId);
    return buildDetail(cycle);
  }

  @Transactional(readOnly = true)
  public LedgerCycleDetailDto getCycleDetail(String groupId, String cycleId) {
    LedgerCycleEntity cycle = requireCycle(cycleId, groupId);
    return buildDetail(cycle);
  }

  public List<LedgerCycleDto> listCycles(String groupId) {
    return ledgerCycleRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
        .map(LedgerCycleDto::from)
        .toList();
  }

  @Transactional
  public LedgerCycleDetailDto closeCycle(String groupId, String actorMemberId, String status) {
    LedgerCycleEntity cycle = ensureOpenCycle(groupId);
    List<SettlementDto> settlements = calculateCycleSettlements(cycle.getId());
    boolean paidFlag = "settled".equals(status);

    for (SettlementDto settlement : settlements) {
      snapshotRepository.save(new SettlementSnapshotEntity(
          IdGenerator.next("snap"), cycle.getId(), settlement.fromMemberId(), settlement.toMemberId(),
          settlement.amount(), paidFlag));
    }

    List<ExpenseEntity> cycleExpenses = expenseRepository.findByLedgerCycleId(cycle.getId());
    LocalDate maxDate = cycle.getStartDate();
    for (ExpenseEntity expense : cycleExpenses) {
      if (expense.getPaidDate().isAfter(maxDate)) {
        maxDate = expense.getPaidDate();
      }
    }
    LocalDate today = LocalDate.now();
    if (today.isAfter(maxDate)) {
      maxDate = today;
    }

    cycle.setStatus(status);
    cycle.setEndDate(maxDate);
    cycle.setClosedAt(Instant.now());
    cycle.setClosedByMemberId(actorMemberId);
    // Flush the status change to the DB now: Hibernate orders inserts before updates
    // within a flush, so without this the new open cycle's insert would race the
    // old cycle's update against the partial unique index on status='open'.
    ledgerCycleRepository.saveAndFlush(cycle);

    String summary = paidFlag ? "Đã tất toán sổ nợ" : "Đã lưu trữ sổ nợ chưa trả";
    String action = paidFlag ? "ledger.settled" : "ledger.archived";
    auditService.log(groupId, cycle.getId(), actorMemberId, action, "ledger_cycle", cycle.getId(), summary, null,
        LedgerCycleDto.from(cycle));

    createOpenCycle(groupId);

    return buildDetail(cycle);
  }

  @Transactional
  public List<SettlementDto> markPaid(String groupId, String cycleId, String settlementId, String actorMemberId) {
    LedgerCycleEntity cycle = requireCycle(cycleId, groupId);
    if (!"open".equals(cycle.getStatus())) {
      throw new ConflictException("Cannot modify settlements in a closed cycle");
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

    String summary = nowPaid ? "Đã đánh dấu khoản nợ là đã trả" : "Đã hoàn tác khoản nợ thành chưa trả";
    String action = nowPaid ? "settlement.marked_paid" : "settlement.marked_unpaid";
    auditService.log(groupId, cycleId, actorMemberId, action, "settlement", settlementId, summary, null, null);

    return calculateCycleSettlements(cycleId);
  }

  @Transactional
  public List<SettlementDto> adjustSettlement(String groupId, String cycleId, String settlementId, long deltaAmount,
      String actorMemberId) {
    LedgerCycleEntity cycle = requireCycle(cycleId, groupId);
    if (!"open".equals(cycle.getStatus())) {
      throw new ConflictException("Cannot modify settlements in a closed cycle");
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
    auditService.log(groupId, cycleId, actorMemberId, "settlement.adjusted", "settlement", settlementId,
        "Đã điều chỉnh nợ " + sign + deltaAmount + "đ",
        Map.of("delta", previousDelta), Map.of("delta", nextDelta));

    return calculateCycleSettlements(cycleId);
  }

  private LedgerCycleDetailDto buildDetail(LedgerCycleEntity cycle) {
    List<ExpenseDto> expenses = expenseRepository.findByLedgerCycleId(cycle.getId()).stream()
        .map(ExpenseMapper::toDto)
        .toList();

    List<SettlementSnapshotDto> settlements;
    if ("open".equals(cycle.getStatus())) {
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

    return new LedgerCycleDetailDto(LedgerCycleDto.from(cycle), expenses, settlements, auditLogs);
  }

  private LedgerCycleEntity requireCycle(String cycleId, String groupId) {
    LedgerCycleEntity cycle = ledgerCycleRepository.findById(cycleId)
        .orElseThrow(() -> new NotFoundException("Ledger cycle not found: " + cycleId));
    if (!cycle.getGroupId().equals(groupId)) {
      throw new NotFoundException("Ledger cycle not found: " + cycleId);
    }
    return cycle;
  }
}
