package com.sharebill.friend;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<FriendshipEntity, String> {
  @Query("select f from FriendshipEntity f where "
      + "(f.requesterUserId = :userA and f.addresseeUserId = :userB) or "
      + "(f.requesterUserId = :userB and f.addresseeUserId = :userA)")
  Optional<FriendshipEntity> findBetween(@Param("userA") String userA, @Param("userB") String userB);

  @Query("select f from FriendshipEntity f where "
      + "(f.requesterUserId = :userId or f.addresseeUserId = :userId) and f.status = 'accepted'")
  List<FriendshipEntity> findAllAcceptedByUserId(@Param("userId") String userId);

  List<FriendshipEntity> findAllByAddresseeUserIdAndStatus(String addresseeUserId, String status);

  List<FriendshipEntity> findAllByRequesterUserIdAndStatus(String requesterUserId, String status);
}
