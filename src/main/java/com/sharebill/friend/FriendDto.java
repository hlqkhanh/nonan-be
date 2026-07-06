package com.sharebill.friend;

public record FriendDto(
    String userId,
    String username,
    String displayName,
    String avatarUrl,
    boolean isFavorite
) {
}
