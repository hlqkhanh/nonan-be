package com.sharebill.group;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MemberDto(
    @NotBlank String id,
    @NotBlank String name,
    String userId
) {
  public static MemberDto from(MemberEntity entity) {
    return new MemberDto(entity.getId(), entity.getName(), entity.getUserId());
  }
}
