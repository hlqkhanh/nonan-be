package com.sharebill.expense;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<ExpenseEntity, String> {
  List<ExpenseEntity> findByOwnerUserIdAndDeletedAtIsNullOrderByPaidDateDesc(String ownerUserId);

  List<ExpenseEntity> findByLedgerCycleIdAndDeletedAtIsNull(String ledgerCycleId);

  /** Unfiltered (includes soft-deleted) — used for FK-safety checks before deleting a cycle row. */
  List<ExpenseEntity> findByLedgerCycleId(String ledgerCycleId);
}
