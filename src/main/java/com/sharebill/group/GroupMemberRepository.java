package com.sharebill.group;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMemberEntity, String> {
  Optional<GroupMemberEntity> findByGroupIdAndTargetTypeAndTargetId(String groupId, String targetType, String targetId);
}
