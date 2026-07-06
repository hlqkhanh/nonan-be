package com.sharebill.group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateGroupRequest(
    @NotBlank String name,
    List<@Valid AddGroupMemberRequest> members
) {
  public List<AddGroupMemberRequest> membersOrEmpty() {
    return members == null ? List.of() : members;
  }
}
