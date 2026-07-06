package com.sharebill.expense;

public final class ExpenseMapper {
  private ExpenseMapper() {
  }

  public static ExpenseDto toDto(ExpenseEntity entity) {
    var payers = entity.getPayers().stream()
        .map(p -> new PayerContributionDto(p.getMemberId(), p.getAmount()))
        .toList();
    var participants = entity.getParticipants().stream()
        .map(p -> new ParticipantShareDto(p.getMemberId(), p.getAmount(), p.isCustom(), p.getMemberName()))
        .toList();

    return new ExpenseDto(
        entity.getId(),
        entity.getGroupId(),
        entity.getTitle(),
        entity.getTotalAmount(),
        entity.getPaidDate(),
        entity.getImageUrl(),
        payers,
        participants,
        entity.getSplitMode(),
        entity.getLedgerCycleId()
    );
  }
}
