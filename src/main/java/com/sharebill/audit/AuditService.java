package com.sharebill.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharebill.common.IdGenerator;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
  private final AuditLogRepository auditLogRepository;
  private final ObjectMapper objectMapper;

  public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
    this.auditLogRepository = auditLogRepository;
    this.objectMapper = objectMapper;
  }

  public AuditLogEntity log(String groupId, String ledgerCycleId, String actorMemberId, String action,
      String entityType, String entityId, String summary, Object before, Object after) {
    JsonNode beforeNode = before == null ? null : objectMapper.valueToTree(before);
    JsonNode afterNode = after == null ? null : objectMapper.valueToTree(after);

    AuditLogEntity entity = new AuditLogEntity(
        IdGenerator.next("audit"),
        groupId,
        ledgerCycleId,
        actorMemberId,
        action,
        entityType,
        entityId,
        summary,
        beforeNode,
        afterNode,
        Instant.now()
    );
    return auditLogRepository.save(entity);
  }
}
