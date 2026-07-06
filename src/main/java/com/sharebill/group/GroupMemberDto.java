package com.sharebill.group;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GroupMemberDto(
    String participantId,
    String name,
    String avatarUrl,
    String type
) {
}
