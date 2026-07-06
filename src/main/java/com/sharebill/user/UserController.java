package com.sharebill.user;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class UserController {
  private final UserService userService;
  private final CloudinaryService cloudinaryService;

  public UserController(UserService userService, CloudinaryService cloudinaryService) {
    this.userService = userService;
    this.cloudinaryService = cloudinaryService;
  }

  @PatchMapping
  public UserDto updateProfile(@Valid @RequestBody UpdateProfileRequest request,
      @AuthenticationPrincipal UserEntity user) {
    return userService.updateProfile(user, request);
  }

  @PostMapping("/password")
  public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
      @AuthenticationPrincipal UserEntity user) {
    userService.changePassword(user, request);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/avatar/signature")
  public AvatarSignatureResponse avatarSignature() {
    return cloudinaryService.createUploadSignature();
  }
}
