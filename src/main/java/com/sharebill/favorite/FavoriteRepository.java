package com.sharebill.favorite;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<FavoriteEntity, String> {
  List<FavoriteEntity> findAllByOwnerUserIdAndTargetType(String ownerUserId, String targetType);

  Optional<FavoriteEntity> findByOwnerUserIdAndTargetTypeAndTargetId(String ownerUserId, String targetType, String targetId);

  void deleteByOwnerUserIdAndTargetTypeAndTargetId(String ownerUserId, String targetType, String targetId);
}
