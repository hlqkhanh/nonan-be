package com.sharebill.ledger;

import java.io.Serializable;
import java.util.Objects;

public class LedgerCycleMemberId implements Serializable {
  private String ledgerCycleId;
  private String userId;

  public LedgerCycleMemberId() {
  }

  public LedgerCycleMemberId(String ledgerCycleId, String userId) {
    this.ledgerCycleId = ledgerCycleId;
    this.userId = userId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LedgerCycleMemberId that)) {
      return false;
    }
    return Objects.equals(ledgerCycleId, that.ledgerCycleId) && Objects.equals(userId, that.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ledgerCycleId, userId);
  }
}
