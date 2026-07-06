package com.sharebill.group;

import com.sharebill.common.ShareBillService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
public class GroupController {
  private final ShareBillService service;

  public GroupController(ShareBillService service) {
    this.service = service;
  }

  @GetMapping
  public List<GroupDto> groups() {
    return service.groups();
  }

  @PostMapping
  public GroupDto createGroup(@Valid @RequestBody GroupDto group) {
    return service.createGroup(group);
  }

  @PostMapping("/{groupId}/members")
  public GroupDto addMember(@PathVariable String groupId, @Valid @RequestBody MemberDto member) {
    return service.addMember(groupId, member);
  }
}
