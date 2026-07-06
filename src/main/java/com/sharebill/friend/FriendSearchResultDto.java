package com.sharebill.friend;

public record FriendSearchResultDto(
    String userId,
    String username,
    String displayName,
    String avatarUrl,
    String relationship
) {
  public static final String RELATIONSHIP_NONE = "none";
  public static final String RELATIONSHIP_FRIEND = "friend";
  public static final String RELATIONSHIP_PENDING_OUTGOING = "pending_outgoing";
  public static final String RELATIONSHIP_PENDING_INCOMING = "pending_incoming";
}
