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

  @Column(name = "group_id", nullable = false)
  private String groupId;

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

  @Column(name = "closed_by_member_id")
  private String closedByMemberId;

  protected LedgerCycleEntity() {
  }

  public LedgerCycleEntity(String id, String groupId, String status, LocalDate startDate, Instant createdAt) {
    this.id = id;
    this.groupId = groupId;
    this.status = status;
    this.startDate = startDate;
    this.createdAt = createdAt;
  }

  public String getId() {
    return id;
  }

  public String getGroupId() {
    return groupId;
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

  public String getClosedByMemberId() {
    return closedByMemberId;
  }

  public void setClosedByMemberId(String closedByMemberId) {
    this.closedByMemberId = closedByMemberId;
  }
}
