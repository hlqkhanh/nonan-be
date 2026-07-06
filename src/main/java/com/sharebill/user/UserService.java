package com.sharebill.user;

import com.sharebill.common.AuthenticationException;
import com.sharebill.common.ConflictException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public UserDto updateProfile(UserEntity user, UpdateProfileRequest request) {
    String username = request.username().trim().toLowerCase();
    if (!username.equals(user.getUsername()) && userRepository.existsByUsername(username)) {
      throw new ConflictException("Username đã được sử dụng: " + username);
    }

    user.setDisplayName(request.displayName().trim());
    user.setUsername(username);
    if (request.avatarUrl() != null) {
      user.setAvatarUrl(request.avatarUrl().isBlank() ? null : request.avatarUrl());
    }

    userRepository.save(user);
    return UserDto.from(user);
  }

  @Transactional
  public void changePassword(UserEntity user, ChangePasswordRequest request) {
    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
      throw new AuthenticationException("Mật khẩu hiện tại không đúng");
    }

    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
  }
}
