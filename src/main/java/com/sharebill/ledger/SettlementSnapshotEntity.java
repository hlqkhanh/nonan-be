package com.sharebill.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "settlement_snapshots")
public class SettlementSnapshotEntity {
  @Id
  private String id;

  @Column(name = "ledger_cycle_id", nullable = false)
  private String ledgerCycleId;

  @Column(name = "from_member_id", nullable = false)
  private String fromMemberId;

  @Column(name = "to_member_id", nullable = false)
  private String toMemberId;

  @Column(nullable = false)
  private long amount;

  @Column(nullable = false)
  private boolean paid;

  protected SettlementSnapshotEntity() {
  }

  public SettlementSnapshotEntity(String id, String ledgerCycleId, String fromMemberId, String toMemberId,
      long amount, boolean paid) {
    this.id = id;
    this.ledgerCycleId = ledgerCycleId;
    this.fromMemberId = fromMemberId;
    this.toMemberId = toMemberId;
    this.amount = amount;
    this.paid = paid;
  }

  public String getId() {
    return id;
  }

  public String getLedgerCycleId() {
    return ledgerCycleId;
  }

  public String getFromMemberId() {
    return fromMemberId;
  }

  public String getToMemberId() {
    return toMemberId;
  }

  public long getAmount() {
    return amount;
  }

  public boolean isPaid() {
    return paid;
  }
}
