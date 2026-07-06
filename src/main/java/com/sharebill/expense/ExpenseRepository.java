package com.sharebill.expense;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<ExpenseEntity, String> {
  List<ExpenseEntity> findByOwnerUserIdOrderByPaidDateDesc(String ownerUserId);

  List<ExpenseEntity> findByLedgerCycleId(String ledgerCycleId);
}
