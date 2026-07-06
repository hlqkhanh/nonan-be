package com.sharebill.ledger;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementSnapshotRepository extends JpaRepository<SettlementSnapshotEntity, String> {
  List<SettlementSnapshotEntity> findByLedgerCycleId(String ledgerCycleId);
}
