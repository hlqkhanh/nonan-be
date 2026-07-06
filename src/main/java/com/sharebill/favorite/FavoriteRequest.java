package com.sharebill.favorite;

import jakarta.validation.constraints.NotBlank;

public record FavoriteRequest(
    @NotBlank String targetType,
    @NotBlank String targetId
) {
}
