package com.sharebill.ledger;

public record SettlementSnapshotDto(
    String id,
    String ledgerCycleId,
    String fromMemberId,
    String toMemberId,
    long amount,
    boolean paid
) {
  public static SettlementSnapshotDto from(SettlementSnapshotEntity entity) {
    return new SettlementSnapshotDto(
        entity.getId(),
        entity.getLedgerCycleId(),
        entity.getFromMemberId(),
        entity.getToMemberId(),
        entity.getAmount(),
        entity.isPaid()
    );
  }
}
