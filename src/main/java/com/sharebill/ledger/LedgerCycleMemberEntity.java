package com.sharebill.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Grants a user visibility into a shared ledger cycle (khoan no) and holds
 * their per-viewer "active" state (which cycle shows on their home screen).
 * Mirrors the group_members shared-membership pattern (V6). Any row here
 * also gates edit/delete/settle access to the cycle's bills for that user.
 */
@Entity
@Table(name = "ledger_cycle_members")
@IdClass(LedgerCycleMemberId.class)
public class LedgerCycleMemberEntity {
  @Id
  @Column(name = "ledger_cycle_id")
  private String ledgerCycleId;

  @Id
  @Column(name = "user_id")
  private String userId;

  @Column(nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected LedgerCycleMemberEntity() {
  }

  public LedgerCycleMemberEntity(String ledgerCycleId, String userId, boolean active, Instant createdAt) {
    this.ledgerCycleId = ledgerCycleId;
    this.userId = userId;
    this.active = active;
    this.createdAt = createdAt;
  }

  public String getLedgerCycleId() {
    return ledgerCycleId;
  }

  public String getUserId() {
    return userId;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
