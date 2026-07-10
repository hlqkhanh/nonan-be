package com.sharebill.ledger;

public record LedgerMemberInfoDto(
    String memberId,
    String displayName,
    String avatarUrl,
    boolean isUser
) {
}
