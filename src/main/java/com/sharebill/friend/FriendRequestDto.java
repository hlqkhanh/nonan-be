package com.sharebill.friend;

import java.time.Instant;

public record FriendRequestDto(
    String id,
    String userId,
    String username,
    String displayName,
    String avatarUrl,
    Instant createdAt
) {
}
