package com.sharebill.group;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<GroupEntity, String> {
  List<GroupEntity> findAllByCreatedByUserIdOrderByCreatedAtAsc(String createdByUserId);
}
