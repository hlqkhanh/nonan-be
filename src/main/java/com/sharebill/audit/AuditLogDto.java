package com.sharebill.audit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditLogDto(
    String id,
    String ownerUserId,
    String ledgerCycleId,
    String action,
    String entityType,
    String entityId,
    String summary,
    JsonNode before,
    JsonNode after,
    Instant createdAt
) {
  public static AuditLogDto from(AuditLogEntity entity) {
    return new AuditLogDto(
        entity.getId(),
        entity.getOwnerUserId(),
        entity.getLedgerCycleId(),
        entity.getAction(),
        entity.getEntityType(),
        entity.getEntityId(),
        entity.getSummary(),
        entity.getBeforeJson(),
        entity.getAfterJson(),
        entity.getCreatedAt()
    );
  }
}
