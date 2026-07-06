package com.sharebill.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank @Size(max = 100) String displayName,
    @NotBlank @Size(max = 50) String username,
    String avatarUrl
) {
}
