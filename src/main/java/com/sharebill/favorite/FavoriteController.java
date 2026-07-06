package com.sharebill.favorite;

import com.sharebill.user.UserEntity;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
  private final FavoriteService favoriteService;

  public FavoriteController(FavoriteService favoriteService) {
    this.favoriteService = favoriteService;
  }

  @PostMapping
  public ResponseEntity<Void> add(@Valid @RequestBody FavoriteRequest request, @AuthenticationPrincipal UserEntity user) {
    favoriteService.add(user.getId(), request.targetType(), request.targetId());
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{targetType}/{targetId}")
  public ResponseEntity<Void> remove(@PathVariable String targetType, @PathVariable String targetId,
      @AuthenticationPrincipal UserEntity user) {
    favoriteService.remove(user.getId(), targetType, targetId);
    return ResponseEntity.noContent().build();
  }
}
