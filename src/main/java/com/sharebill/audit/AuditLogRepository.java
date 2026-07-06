package com.sharebill.audit;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, String> {
  List<AuditLogEntity> findByLedgerCycleIdOrderByCreatedAtAsc(String ledgerCycleId);
}
