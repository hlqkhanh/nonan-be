package com.sharebill.expense;

import com.sharebill.user.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class ExpenseMapper {
  private final ParticipantResolver participantResolver;
  private final UserRepository userRepository;

  public ExpenseMapper(ParticipantResolver participantResolver, UserRepository userRepository) {
    this.participantResolver = participantResolver;
    this.userRepository = userRepository;
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

    String createdByDisplayName = userRepository.findById(entity.getOwnerUserId())
        .map(u -> u.getDisplayName())
        .orElse(null);

    return new ExpenseDto(
        entity.getId(),
        entity.getTitle(),
        entity.getTotalAmount(),
        entity.getPaidDate(),
        entity.getImageUrl(),
        payers,
        participants,
        entity.getSplitMode(),
        entity.getLedgerCycleId(),
        createdByDisplayName
    );
  }
}
