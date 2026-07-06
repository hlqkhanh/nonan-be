package com.sharebill.settlement;

import jakarta.validation.constraints.NotBlank;

public record AdjustRequest(
    @NotBlank String settlementId,
    long deltaAmount
) {
}
