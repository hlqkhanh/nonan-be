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
    String closedByUserId,
    Boolean active,
    Boolean isOwner,
    String ownerDisplayName,
    String ownerAvatarUrl,
    Long viewerNet,
    Long totalAmount,
    Integer unpaidCount
) {
  /** Viewer-agnostic mapping (used where there is no specific viewer, e.g. audit-log payloads). */
  public static LedgerCycleDto from(LedgerCycleEntity entity) {
    return new LedgerCycleDto(
        entity.getId(),
        entity.getOwnerUserId(),
        entity.getStatus(),
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getCreatedAt(),
        entity.getClosedAt(),
        entity.getClosedByUserId(),
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );
  }

  /** Viewer-relative mapping — sets active/isOwner/owner display info for the requesting user. */
  public static LedgerCycleDto from(LedgerCycleEntity entity, boolean active, boolean isOwner,
      String ownerDisplayName, String ownerAvatarUrl) {
    return new LedgerCycleDto(
        entity.getId(),
        entity.getOwnerUserId(),
        entity.getStatus(),
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getCreatedAt(),
        entity.getClosedAt(),
        entity.getClosedByUserId(),
        active,
        isOwner,
        ownerDisplayName,
        ownerAvatarUrl,
        null,
        null,
        null
    );
  }

  /** Viewer-relative mapping enriched with balance-overview fields for card listings (Issue 3). */
  public static LedgerCycleDto withSummary(LedgerCycleEntity entity, boolean active, boolean isOwner,
      String ownerDisplayName, String ownerAvatarUrl, long viewerNet, long totalAmount, int unpaidCount) {
    return new LedgerCycleDto(
        entity.getId(),
        entity.getOwnerUserId(),
        entity.getStatus(),
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getCreatedAt(),
        entity.getClosedAt(),
        entity.getClosedByUserId(),
        active,
        isOwner,
        ownerDisplayName,
        ownerAvatarUrl,
        viewerNet,
        totalAmount,
        unpaidCount
    );
  }
}
