package com.sharebill.group;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<MemberEntity, String> {
  Optional<MemberEntity> findByGroupIdAndUserId(String groupId, String userId);
}
