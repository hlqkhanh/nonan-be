package com.sharebill.ledger;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaidSettlementRepository extends JpaRepository<PaidSettlementEntity, Long> {
  List<PaidSettlementEntity> findByLedgerCycleId(String ledgerCycleId);

  Optional<PaidSettlementEntity> findByLedgerCycleIdAndPairKey(String ledgerCycleId, String pairKey);
}
