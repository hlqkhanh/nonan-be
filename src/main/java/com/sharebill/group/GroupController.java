package com.sharebill.group;

import com.sharebill.user.UserEntity;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
public class GroupController {
  private final GroupService groupService;

  public GroupController(GroupService groupService) {
    this.groupService = groupService;
  }

  @GetMapping
  public List<GroupDto> groups(@AuthenticationPrincipal UserEntity user) {
    return groupService.listGroups(user.getId());
  }

  @PostMapping
  public GroupDto createGroup(@Valid @RequestBody CreateGroupRequest request, @AuthenticationPrincipal UserEntity user) {
    return groupService.createGroup(user, request);
  }

  @PatchMapping("/{groupId}")
  public GroupDto renameGroup(@PathVariable String groupId, @Valid @RequestBody RenameGroupRequest request,
      @AuthenticationPrincipal UserEntity user) {
    return groupService.renameGroup(user.getId(), groupId, request);
  }

  @PostMapping("/{groupId}/members")
  public GroupDto addMember(@PathVariable String groupId, @Valid @RequestBody AddGroupMemberRequest request,
      @AuthenticationPrincipal UserEntity user) {
    return groupService.addMember(user.getId(), groupId, request);
  }

  @DeleteMapping("/{groupId}/members/{targetType}/{targetId}")
  public GroupDto removeMember(@PathVariable String groupId, @PathVariable String targetType,
      @PathVariable String targetId, @AuthenticationPrincipal UserEntity user) {
    return groupService.removeMember(user.getId(), groupId, targetType, targetId);
  }

  @DeleteMapping("/{groupId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteGroup(@PathVariable String groupId, @AuthenticationPrincipal UserEntity user) {
    groupService.deleteGroup(user.getId(), groupId);
  }
}
