package com.sharebill.expense;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<ExpenseEntity, String> {
  List<ExpenseEntity> findByGroupIdOrderByPaidDateDesc(String groupId);

  List<ExpenseEntity> findByLedgerCycleId(String ledgerCycleId);
}
