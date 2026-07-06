package com.sharebill.ledger;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerCycleRepository extends JpaRepository<LedgerCycleEntity, String> {
  Optional<LedgerCycleEntity> findByOwnerUserIdAndStatus(String ownerUserId, String status);

  List<LedgerCycleEntity> findByOwnerUserIdOrderByCreatedAtDesc(String ownerUserId);
}
