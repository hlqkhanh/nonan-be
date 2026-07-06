package com.sharebill.expense;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PayerContributionDto(
    @NotBlank String memberId,
    @Min(0) long amount
) {
}
