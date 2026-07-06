package com.sharebill.settlement;

public record SettlementDto(
    String id,
    String fromMemberId,
    String toMemberId,
    long amount,
    boolean paid
) {
}
