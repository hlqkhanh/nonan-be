package com.sharebill.friend;

import com.sharebill.user.UserEntity;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/friends")
public class FriendController {
  private final FriendService friendService;

  public FriendController(FriendService friendService) {
    this.friendService = friendService;
  }

  @GetMapping
  public List<FriendDto> friends(@AuthenticationPrincipal UserEntity user) {
    return friendService.listFriends(user.getId());
  }

  @GetMapping("/requests")
  public FriendRequestsResponse requests(@AuthenticationPrincipal UserEntity user) {
    return friendService.listRequests(user.getId());
  }

  @GetMapping("/search")
  public List<FriendSearchResultDto> search(@RequestParam("q") String query, @AuthenticationPrincipal UserEntity user) {
    return friendService.search(user.getId(), query);
  }

  @PostMapping("/requests")
  public ResponseEntity<Void> sendRequest(@Valid @RequestBody SendFriendRequestRequest request,
      @AuthenticationPrincipal UserEntity user) {
    friendService.sendRequest(user.getId(), request);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/requests/{requestId}/accept")
  public ResponseEntity<Void> accept(@PathVariable String requestId, @AuthenticationPrincipal UserEntity user) {
    friendService.acceptRequest(user.getId(), requestId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/requests/{requestId}/reject")
  public ResponseEntity<Void> reject(@PathVariable String requestId, @AuthenticationPrincipal UserEntity user) {
    friendService.rejectRequest(user.getId(), requestId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{friendUserId}")
  public ResponseEntity<Void> remove(@PathVariable String friendUserId, @AuthenticationPrincipal UserEntity user) {
    friendService.removeFriend(user.getId(), friendUserId);
    return ResponseEntity.noContent().build();
  }
}
