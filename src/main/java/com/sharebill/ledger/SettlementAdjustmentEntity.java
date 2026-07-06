package com.sharebill.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "settlement_adjustments")
public class SettlementAdjustmentEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "ledger_cycle_id", nullable = false)
  private String ledgerCycleId;

  @Column(name = "pair_key", nullable = false)
  private String pairKey;

  @Column(nullable = false)
  private long delta;

  protected SettlementAdjustmentEntity() {
  }

  public SettlementAdjustmentEntity(String ledgerCycleId, String pairKey, long delta) {
    this.ledgerCycleId = ledgerCycleId;
    this.pairKey = pairKey;
    this.delta = delta;
  }

  public Long getId() {
    return id;
  }

  public String getLedgerCycleId() {
    return ledgerCycleId;
  }

  public String getPairKey() {
    return pairKey;
  }

  public long getDelta() {
    return delta;
  }

  public void setDelta(long delta) {
    this.delta = delta;
  }
}
