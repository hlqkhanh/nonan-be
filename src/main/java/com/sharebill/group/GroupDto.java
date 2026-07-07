package com.sharebill.group;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record GroupDto(
    @NotBlank String id,
    @NotBlank String name,
    List<GroupMemberDto> members,
    String createdByUserId
) {
}
