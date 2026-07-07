package com.sharebill.group;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupRepository extends JpaRepository<GroupEntity, String> {
  @Query("select distinct g from GroupEntity g left join g.members m "
      + "where g.createdByUserId = :userId "
      + "or (m.targetType = 'user' and m.targetId = :userId) "
      + "order by g.createdAt asc")
  List<GroupEntity> findAllAccessibleByUser(@Param("userId") String userId);
}
