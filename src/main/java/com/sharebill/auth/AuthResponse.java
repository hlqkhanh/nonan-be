package com.sharebill.auth;

import com.sharebill.user.UserDto;

public record AuthResponse(
    String token,
    UserDto user
) {
}
