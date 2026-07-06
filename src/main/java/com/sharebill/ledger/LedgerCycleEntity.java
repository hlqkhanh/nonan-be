package com.sharebill.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "ledger_cycles")
public class LedgerCycleEntity {
  @Id
  private String id;

  @Column(name = "owner_user_id", nullable = false)
  private String ownerUserId;

  @Column(nullable = false)
  private String status;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "closed_at")
  private Instant closedAt;

  @Column(name = "closed_by_user_id")
  private String closedByUserId;

  protected LedgerCycleEntity() {
  }

  public LedgerCycleEntity(String id, String ownerUserId, String status, LocalDate startDate, Instant createdAt) {
    this.id = id;
    this.ownerUserId = ownerUserId;
    this.status = status;
    this.startDate = startDate;
    this.createdAt = createdAt;
  }

  public String getId() {
    return id;
  }

  public String getOwnerUserId() {
    return ownerUserId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getClosedAt() {
    return closedAt;
  }

  public void setClosedAt(Instant closedAt) {
    this.closedAt = closedAt;
  }

  public String getClosedByUserId() {
    return closedByUserId;
  }

  public void setClosedByUserId(String closedByUserId) {
    this.closedByUserId = closedByUserId;
  }
}
