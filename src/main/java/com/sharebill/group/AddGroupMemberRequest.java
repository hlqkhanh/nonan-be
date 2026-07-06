package com.sharebill.group;

import jakarta.validation.constraints.NotBlank;

public record AddGroupMemberRequest(
    @NotBlank String targetType,
    @NotBlank String targetId
) {
}
