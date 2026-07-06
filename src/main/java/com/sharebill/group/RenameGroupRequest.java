package com.sharebill.group;

import jakarta.validation.constraints.NotBlank;

public record RenameGroupRequest(
    @NotBlank String name
) {
}
