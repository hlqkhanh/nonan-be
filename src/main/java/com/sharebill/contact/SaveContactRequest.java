package com.sharebill.contact;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveContactRequest(
    @NotBlank @Size(max = 100) String name,
    String avatarUrl
) {
}
