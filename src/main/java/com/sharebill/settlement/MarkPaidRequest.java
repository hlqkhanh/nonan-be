package com.sharebill.settlement;

import jakarta.validation.constraints.NotBlank;

public record MarkPaidRequest(
    @NotBlank String settlementId
) {
}
