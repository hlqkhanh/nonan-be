package com.sharebill.favorite;

import com.sharebill.common.IdGenerator;
import com.sharebill.common.NotFoundException;
import com.sharebill.contact.ContactRepository;
import com.sharebill.friend.FriendshipEntity;
import com.sharebill.friend.FriendshipRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteService {
  private final FavoriteRepository favoriteRepository;
  private final FriendshipRepository friendshipRepository;
  private final ContactRepository contactRepository;

  public FavoriteService(FavoriteRepository favoriteRepository, FriendshipRepository friendshipRepository,
      ContactRepository contactRepository) {
    this.favoriteRepository = favoriteRepository;
    this.friendshipRepository = friendshipRepository;
    this.contactRepository = contactRepository;
  }

  @Transactional
  public void add(String ownerUserId, String targetType, String targetId) {
    if (!FavoriteTargetType.isValid(targetType)) {
      throw new IllegalArgumentException("targetType không hợp lệ: " + targetType);
    }
    validateTargetBelongsToOwner(ownerUserId, targetType, targetId);

    if (favoriteRepository.findByOwnerUserIdAndTargetTypeAndTargetId(ownerUserId, targetType, targetId).isPresent()) {
      return;
    }

    favoriteRepository.save(new FavoriteEntity(IdGenerator.next("favorite"), ownerUserId, targetType, targetId, Instant.now()));
  }

  @Transactional
  public void remove(String ownerUserId, String targetType, String targetId) {
    favoriteRepository.deleteByOwnerUserIdAndTargetTypeAndTargetId(ownerUserId, targetType, targetId);
  }

  private void validateTargetBelongsToOwner(String ownerUserId, String targetType, String targetId) {
    if (FavoriteTargetType.USER.equals(targetType)) {
      friendshipRepository.findBetween(ownerUserId, targetId)
          .filter(friendship -> FriendshipEntity.STATUS_ACCEPTED.equals(friendship.getStatus()))
          .orElseThrow(() -> new NotFoundException("Không tìm thấy bạn bè"));
    } else {
      contactRepository.findById(targetId)
          .filter(contact -> contact.getOwnerUserId().equals(ownerUserId))
          .orElseThrow(() -> new NotFoundException("Không tìm thấy liên hệ"));
    }
  }
}
