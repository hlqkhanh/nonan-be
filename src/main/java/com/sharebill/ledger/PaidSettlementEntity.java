package com.sharebill.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "paid_settlements")
public class PaidSettlementEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "ledger_cycle_id", nullable = false)
  private String ledgerCycleId;

  @Column(name = "pair_key", nullable = false)
  private String pairKey;

  protected PaidSettlementEntity() {
  }

  public PaidSettlementEntity(String ledgerCycleId, String pairKey) {
    this.ledgerCycleId = ledgerCycleId;
    this.pairKey = pairKey;
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
}
