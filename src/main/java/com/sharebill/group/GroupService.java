package com.sharebill.group;

import com.sharebill.common.ForbiddenException;
import com.sharebill.common.IdGenerator;
import com.sharebill.common.NotFoundException;
import com.sharebill.contact.ContactRepository;
import com.sharebill.friend.FriendshipEntity;
import com.sharebill.friend.FriendshipRepository;
import com.sharebill.user.UserEntity;
import com.sharebill.user.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupService {
  private final GroupRepository groupRepository;
  private final GroupMemberRepository groupMemberRepository;
  private final UserRepository userRepository;
  private final ContactRepository contactRepository;
  private final FriendshipRepository friendshipRepository;

  public GroupService(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository,
      UserRepository userRepository, ContactRepository contactRepository, FriendshipRepository friendshipRepository) {
    this.groupRepository = groupRepository;
    this.groupMemberRepository = groupMemberRepository;
    this.userRepository = userRepository;
    this.contactRepository = contactRepository;
    this.friendshipRepository = friendshipRepository;
  }

  @Transactional(readOnly = true)
  public List<GroupDto> listGroups(String userId) {
    return groupRepository.findAllByCreatedByUserIdOrderByCreatedAtAsc(userId).stream()
        .map(this::toDto)
        .toList();
  }

  @Transactional
  public GroupDto createGroup(UserEntity creator, CreateGroupRequest request) {
    GroupEntity group = new GroupEntity(IdGenerator.next("group"), request.name().trim(), creator.getId(), Instant.now());
    for (AddGroupMemberRequest member : request.membersOrEmpty()) {
      attachMember(creator.getId(), group, member);
    }
    groupRepository.save(group);
    return toDto(group);
  }

  @Transactional
  public GroupDto renameGroup(String userId, String groupId, RenameGroupRequest request) {
    GroupEntity group = requireOwnedGroup(userId, groupId);
    group.setName(request.name().trim());
    groupRepository.save(group);
    return toDto(group);
  }

  @Transactional
  public GroupDto addMember(String userId, String groupId, AddGroupMemberRequest request) {
    GroupEntity group = requireOwnedGroup(userId, groupId);
    attachMember(userId, group, request);
    groupRepository.save(group);
    return toDto(groupRepository.findById(groupId).orElseThrow());
  }

  @Transactional
  public GroupDto removeMember(String userId, String groupId, String targetType, String targetId) {
    GroupEntity group = requireOwnedGroup(userId, groupId);
    GroupMemberEntity member = groupMemberRepository.findByGroupIdAndTargetTypeAndTargetId(groupId, targetType, targetId)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy thành viên trong nhóm"));
    group.getMembers().remove(member);
    groupMemberRepository.delete(member);
    return toDto(groupRepository.findById(groupId).orElseThrow());
  }

  @Transactional
  public void deleteGroup(String userId, String groupId) {
    GroupEntity group = requireOwnedGroup(userId, groupId);
    groupRepository.delete(group);
  }

  private void attachMember(String ownerUserId, GroupEntity group, AddGroupMemberRequest request) {
    String targetType = request.targetType();
    String targetId = request.targetId();
    if (!"user".equals(targetType) && !"contact".equals(targetType)) {
      throw new IllegalArgumentException("targetType không hợp lệ: " + targetType);
    }
    validateTargetBelongsToOwner(ownerUserId, targetType, targetId);

    boolean alreadyPresent = group.getMembers().stream()
        .anyMatch(m -> m.getTargetType().equals(targetType) && m.getTargetId().equals(targetId));
    if (alreadyPresent) {
      return;
    }

    group.getMembers().add(new GroupMemberEntity(IdGenerator.next("gmember"), group, targetType, targetId, Instant.now()));
  }

  private void validateTargetBelongsToOwner(String ownerUserId, String targetType, String targetId) {
    if ("user".equals(targetType)) {
      if (targetId.equals(ownerUserId)) {
        return;
      }
      friendshipRepository.findBetween(ownerUserId, targetId)
          .filter(friendship -> FriendshipEntity.STATUS_ACCEPTED.equals(friendship.getStatus()))
          .orElseThrow(() -> new NotFoundException("Không tìm thấy bạn bè"));
    } else {
      contactRepository.findById(targetId)
          .filter(contact -> contact.getOwnerUserId().equals(ownerUserId))
          .orElseThrow(() -> new NotFoundException("Không tìm thấy liên hệ"));
    }
  }

  private GroupEntity requireOwnedGroup(String userId, String groupId) {
    GroupEntity group = groupRepository.findById(groupId)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy nhóm: " + groupId));
    if (!userId.equals(group.getCreatedByUserId())) {
      throw new ForbiddenException("Bạn không có quyền với nhóm này");
    }
    return group;
  }

  private GroupDto toDto(GroupEntity group) {
    List<GroupMemberDto> members = group.getMembers().stream()
        .map(this::resolveMember)
        .toList();
    return new GroupDto(group.getId(), group.getName(), members);
  }

  private GroupMemberDto resolveMember(GroupMemberEntity member) {
    String participantId = member.getTargetType() + ":" + member.getTargetId();
    if ("user".equals(member.getTargetType())) {
      return userRepository.findById(member.getTargetId())
          .map(u -> new GroupMemberDto(participantId, u.getDisplayName(), u.getAvatarUrl(), "user"))
          .orElse(new GroupMemberDto(participantId, "(Người dùng đã xóa)", null, "user"));
    }
    return contactRepository.findById(member.getTargetId())
        .map(c -> new GroupMemberDto(participantId, c.getName(), c.getAvatarUrl(), "contact"))
        .orElse(new GroupMemberDto(participantId, "(Liên hệ đã xóa)", null, "contact"));
  }
}
