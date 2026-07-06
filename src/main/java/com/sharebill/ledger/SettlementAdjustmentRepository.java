package com.sharebill.ledger;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementAdjustmentRepository extends JpaRepository<SettlementAdjustmentEntity, Long> {
  List<SettlementAdjustmentEntity> findByLedgerCycleId(String ledgerCycleId);

  Optional<SettlementAdjustmentEntity> findByLedgerCycleIdAndPairKey(String ledgerCycleId, String pairKey);
}
