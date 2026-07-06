package com.sharebill.expense;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ParticipantShareDto(
    @NotBlank String memberId,
    @Min(0) long amount,
    boolean isCustom,
    String memberName
) {
}
