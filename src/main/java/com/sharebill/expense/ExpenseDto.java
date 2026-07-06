package com.sharebill.expense;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExpenseDto(
    @NotBlank String id,
    @NotBlank String title,
    @Min(0) long totalAmount,
    LocalDate paidDate,
    String imageUrl,
    @NotEmpty List<@Valid PayerContributionDto> payers,
    @NotEmpty List<@Valid ParticipantShareDto> participants,
    @NotBlank String splitMode,
    String ledgerCycleId
) {
}
