package com.sharebill.expense;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PayerContributionDto(
    @NotBlank String memberId,
    @Min(0) long amount,
    String name,
    String avatarUrl
) {
  public PayerContributionDto(String memberId, long amount) {
    this(memberId, amount, null, null);
  }
}
