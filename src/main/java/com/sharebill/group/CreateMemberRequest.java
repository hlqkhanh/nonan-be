package com.sharebill.group;

import jakarta.validation.constraints.NotBlank;

public record CreateMemberRequest(
    String id,
    @NotBlank String name
) {
}
