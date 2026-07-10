package com.sharebill.ledger;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerCycleMemberRepository extends JpaRepository<LedgerCycleMemberEntity, LedgerCycleMemberId> {
  List<LedgerCycleMemberEntity> findByUserId(String userId);

  List<LedgerCycleMemberEntity> findByLedgerCycleId(String ledgerCycleId);

  boolean existsByLedgerCycleIdAndUserId(String ledgerCycleId, String userId);

  Optional<LedgerCycleMemberEntity> findByLedgerCycleIdAndUserId(String ledgerCycleId, String userId);

  Optional<LedgerCycleMemberEntity> findByUserIdAndActiveTrue(String userId);
}
