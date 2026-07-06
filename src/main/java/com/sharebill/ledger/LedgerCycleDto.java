package com.sharebill.ledger;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LedgerCycleDto(
    String id,
    String ownerUserId,
    String status,
    LocalDate startDate,
    LocalDate endDate,
    Instant createdAt,
    Instant closedAt,
    String closedByUserId
) {
  public static LedgerCycleDto from(LedgerCycleEntity entity) {
    return new LedgerCycleDto(
        entity.getId(),
        entity.getOwnerUserId(),
        entity.getStatus(),
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getCreatedAt(),
        entity.getClosedAt(),
        entity.getClosedByUserId()
    );
  }
}
