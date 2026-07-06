package com.sharebill.ledger;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LedgerCycleDto(
    String id,
    String groupId,
    String status,
    LocalDate startDate,
    LocalDate endDate,
    Instant createdAt,
    Instant closedAt,
    String closedByMemberId
) {
  public static LedgerCycleDto from(LedgerCycleEntity entity) {
    return new LedgerCycleDto(
        entity.getId(),
        entity.getGroupId(),
        entity.getStatus(),
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getCreatedAt(),
        entity.getClosedAt(),
        entity.getClosedByMemberId()
    );
  }
}
