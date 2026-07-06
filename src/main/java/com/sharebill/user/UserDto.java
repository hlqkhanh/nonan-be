package com.sharebill.user;

public record UserDto(
    String id,
    String email,
    String username,
    String displayName,
    String avatarUrl
) {
  public static UserDto from(UserEntity entity) {
    return new UserDto(entity.getId(), entity.getEmail(), entity.getUsername(), entity.getDisplayName(),
        entity.getAvatarUrl());
  }
}
