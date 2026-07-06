package com.sharebill.expense;

import org.springframework.stereotype.Component;

@Component
public class ExpenseMapper {
  private final ParticipantResolver participantResolver;

  public ExpenseMapper(ParticipantResolver participantResolver) {
    this.participantResolver = participantResolver;
  }

  public ExpenseDto toDto(ExpenseEntity entity) {
    var payers = entity.getPayers().stream()
        .map(p -> {
          var resolved = participantResolver.resolve(p.getMemberId());
          return new PayerContributionDto(p.getMemberId(), p.getAmount(), resolved.name(), resolved.avatarUrl());
        })
        .toList();
    var participants = entity.getParticipants().stream()
        .map(p -> {
          var resolved = participantResolver.resolve(p.getMemberId());
          return new ParticipantShareDto(p.getMemberId(), p.getAmount(), p.isCustom(), resolved.name(), resolved.avatarUrl());
        })
        .toList();

    return new ExpenseDto(
        entity.getId(),
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
