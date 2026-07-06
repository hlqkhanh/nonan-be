package com.sharebill.group;

import com.sharebill.common.ForbiddenException;
import com.sharebill.common.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GroupAccessService {
  private final GroupRepository groupRepository;
  private final MemberRepository memberRepository;

  public GroupAccessService(GroupRepository groupRepository, MemberRepository memberRepository) {
    this.groupRepository = groupRepository;
    this.memberRepository = memberRepository;
  }

  public GroupEntity requireGroup(String groupId) {
    return groupRepository.findById(groupId)
        .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));
  }

  public MemberEntity requireMember(String groupId, String userId) {
    requireGroup(groupId);
    return memberRepository.findByGroupIdAndUserId(groupId, userId)
        .orElseThrow(() -> new ForbiddenException("Not a member of group: " + groupId));
  }
}
