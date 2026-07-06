package com.sharebill.contact;

public record ContactDto(
    String id,
    String name,
    String avatarUrl,
    boolean isFavorite
) {
}
