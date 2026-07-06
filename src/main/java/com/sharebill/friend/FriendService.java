package com.sharebill.friend;

import com.sharebill.common.ConflictException;
import com.sharebill.common.ForbiddenException;
import com.sharebill.common.IdGenerator;
import com.sharebill.common.NotFoundException;
import com.sharebill.favorite.FavoriteRepository;
import com.sharebill.favorite.FavoriteTargetType;
import com.sharebill.user.UserEntity;
import com.sharebill.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendService {
  private final FriendshipRepository friendshipRepository;
  private final UserRepository userRepository;
  private final FavoriteRepository favoriteRepository;

  public FriendService(FriendshipRepository friendshipRepository, UserRepository userRepository,
      FavoriteRepository favoriteRepository) {
    this.friendshipRepository = friendshipRepository;
    this.userRepository = userRepository;
    this.favoriteRepository = favoriteRepository;
  }

  @Transactional(readOnly = true)
  public List<FriendDto> listFriends(String userId) {
    Set<String> favoriteUserIds = favoriteRepository.findAllByOwnerUserIdAndTargetType(userId, FavoriteTargetType.USER)
        .stream()
        .map(favorite -> favorite.getTargetId())
        .collect(Collectors.toSet());

    return friendshipRepository.findAllAcceptedByUserId(userId).stream()
        .map(friendship -> {
          String otherUserId = friendship.otherUserId(userId);
          UserEntity other = userRepository.findById(otherUserId).orElseThrow();
          return new FriendDto(other.getId(), other.getUsername(), other.getDisplayName(), other.getAvatarUrl(),
              favoriteUserIds.contains(other.getId()));
        })
        .toList();
  }

  @Transactional(readOnly = true)
  public FriendRequestsResponse listRequests(String userId) {
    List<FriendRequestDto> incoming = friendshipRepository
        .findAllByAddresseeUserIdAndStatus(userId, FriendshipEntity.STATUS_PENDING).stream()
        .map(friendship -> toRequestDto(friendship, friendship.getRequesterUserId()))
        .toList();
    List<FriendRequestDto> outgoing = friendshipRepository
        .findAllByRequesterUserIdAndStatus(userId, FriendshipEntity.STATUS_PENDING).stream()
        .map(friendship -> toRequestDto(friendship, friendship.getAddresseeUserId()))
        .toList();
    return new FriendRequestsResponse(incoming, outgoing);
  }

  private FriendRequestDto toRequestDto(FriendshipEntity friendship, String otherUserId) {
    UserEntity other = userRepository.findById(otherUserId).orElseThrow();
    return new FriendRequestDto(friendship.getId(), other.getId(), other.getUsername(), other.getDisplayName(),
        other.getAvatarUrl(), friendship.getCreatedAt());
  }

  @Transactional(readOnly = true)
  public List<FriendSearchResultDto> search(String userId, String query) {
    String normalized = query.trim().toLowerCase();
    if (normalized.isBlank()) {
      return List.of();
    }

    return userRepository.findTop20ByUsernameContainingIgnoreCaseAndIdNot(normalized, userId).stream()
        .map(candidate -> {
          String relationship = friendshipRepository.findBetween(userId, candidate.getId())
              .map(friendship -> {
                if (FriendshipEntity.STATUS_ACCEPTED.equals(friendship.getStatus())) {
                  return FriendSearchResultDto.RELATIONSHIP_FRIEND;
                }
                return friendship.getRequesterUserId().equals(userId)
                    ? FriendSearchResultDto.RELATIONSHIP_PENDING_OUTGOING
                    : FriendSearchResultDto.RELATIONSHIP_PENDING_INCOMING;
              })
              .orElse(FriendSearchResultDto.RELATIONSHIP_NONE);
          return new FriendSearchResultDto(candidate.getId(), candidate.getUsername(), candidate.getDisplayName(),
              candidate.getAvatarUrl(), relationship);
        })
        .toList();
  }

  @Transactional
  public void sendRequest(String requesterId, SendFriendRequestRequest request) {
    String username = request.username().trim().toLowerCase();
    UserEntity target = userRepository.findByUsername(username)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy user: " + username));

    if (target.getId().equals(requesterId)) {
      throw new ConflictException("Không thể tự kết bạn với chính mình");
    }

    friendshipRepository.findBetween(requesterId, target.getId()).ifPresent(existing -> {
      if (FriendshipEntity.STATUS_ACCEPTED.equals(existing.getStatus())) {
        throw new ConflictException("Đã là bạn bè");
      }
      throw new ConflictException("Đã có lời mời kết bạn đang chờ xử lý");
    });

    FriendshipEntity friendship = new FriendshipEntity(
        IdGenerator.next("friendship"), requesterId, target.getId(), FriendshipEntity.STATUS_PENDING, Instant.now());
    friendshipRepository.save(friendship);
  }

  @Transactional
  public void acceptRequest(String userId, String requestId) {
    FriendshipEntity friendship = friendshipRepository.findById(requestId)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy lời mời kết bạn"));

    if (!friendship.getAddresseeUserId().equals(userId)) {
      throw new ForbiddenException("Bạn không thể chấp nhận lời mời này");
    }
    if (!FriendshipEntity.STATUS_PENDING.equals(friendship.getStatus())) {
      throw new ConflictException("Lời mời không còn ở trạng thái chờ");
    }

    friendship.setStatus(FriendshipEntity.STATUS_ACCEPTED);
    friendship.setRespondedAt(Instant.now());
    friendshipRepository.save(friendship);
  }

  @Transactional
  public void rejectRequest(String userId, String requestId) {
    FriendshipEntity friendship = friendshipRepository.findById(requestId)
        .orElseThrow(() -> new NotFoundException("Không tìm thấy lời mời kết bạn"));

    if (!friendship.getAddresseeUserId().equals(userId)) {
      throw new ForbiddenException("Bạn không thể từ chối lời mời này");
    }
    if (!FriendshipEntity.STATUS_PENDING.equals(friendship.getStatus())) {
      throw new ConflictException("Lời mời không còn ở trạng thái chờ");
    }

    friendshipRepository.delete(friendship);
  }

  @Transactional
  public void removeFriend(String userId, String friendUserId) {
    FriendshipEntity friendship = friendshipRepository.findBetween(userId, friendUserId)
        .filter(f -> FriendshipEntity.STATUS_ACCEPTED.equals(f.getStatus()))
        .orElseThrow(() -> new NotFoundException("Không tìm thấy bạn bè"));

    friendshipRepository.delete(friendship);
    favoriteRepository.deleteByOwnerUserIdAndTargetTypeAndTargetId(userId, FavoriteTargetType.USER, friendUserId);
    favoriteRepository.deleteByOwnerUserIdAndTargetTypeAndTargetId(friendUserId, FavoriteTargetType.USER, userId);
  }
}
