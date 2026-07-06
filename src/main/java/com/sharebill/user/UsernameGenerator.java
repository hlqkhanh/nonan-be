package com.sharebill.user;

import org.springframework.stereotype.Component;

@Component
public class UsernameGenerator {
  private final UserRepository userRepository;

  public UsernameGenerator(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public String generateFromEmail(String email) {
    String localPart = email.substring(0, Math.max(email.indexOf('@'), 0));
    String base = localPart.toLowerCase().replaceAll("[^a-z0-9]", "");
    if (base.isBlank()) {
      base = "user";
    }

    String candidate = base;
    int suffix = 1;
    while (userRepository.existsByUsername(candidate)) {
      suffix++;
      candidate = base + suffix;
    }
    return candidate;
  }
}
