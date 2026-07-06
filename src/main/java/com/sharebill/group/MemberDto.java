package com.sharebill.group;

import jakarta.validation.constraints.NotBlank;

public record MemberDto(
    @NotBlank String id,
    @NotBlank String name
) {
}
