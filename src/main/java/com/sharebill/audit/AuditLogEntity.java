package com.sharebill.audit;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {
  @Id
  private String id;

  @Column(name = "owner_user_id", nullable = false)
  private String ownerUserId;

  @Column(name = "ledger_cycle_id")
  private String ledgerCycleId;

  @Column(nullable = false)
  private String action;

  @Column(name = "entity_type", nullable = false)
  private String entityType;

  @Column(name = "entity_id", nullable = false)
  private String entityId;

  @Column(nullable = false, columnDefinition = "text")
  private String summary;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "before_json")
  private JsonNode beforeJson;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "after_json")
  private JsonNode afterJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected AuditLogEntity() {
  }

  public AuditLogEntity(String id, String ownerUserId, String ledgerCycleId, String action,
      String entityType, String entityId, String summary, JsonNode beforeJson, JsonNode afterJson, Instant createdAt) {
    this.id = id;
    this.ownerUserId = ownerUserId;
    this.ledgerCycleId = ledgerCycleId;
    this.action = action;
    this.entityType = entityType;
    this.entityId = entityId;
    this.summary = summary;
    this.beforeJson = beforeJson;
    this.afterJson = afterJson;
    this.createdAt = createdAt;
  }

  public String getId() {
    return id;
  }

  public String getOwnerUserId() {
    return ownerUserId;
  }

  public String getLedgerCycleId() {
    return ledgerCycleId;
  }

  public String getAction() {
    return action;
  }

  public String getEntityType() {
    return entityType;
  }

  public String getEntityId() {
    return entityId;
  }

  public String getSummary() {
    return summary;
  }

  public JsonNode getBeforeJson() {
    return beforeJson;
  }

  public JsonNode getAfterJson() {
    return afterJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
