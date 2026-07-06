package com.sharebill.friend;

import jakarta.validation.constraints.NotBlank;

public record SendFriendRequestRequest(
    @NotBlank String username
) {
}
