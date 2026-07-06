package com.sharebill.ledger;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerCycleRepository extends JpaRepository<LedgerCycleEntity, String> {
  Optional<LedgerCycleEntity> findByGroupIdAndStatus(String groupId, String status);

  List<LedgerCycleEntity> findByGroupIdOrderByCreatedAtDesc(String groupId);
}
