package com.sharebill.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {
  Optional<UserEntity> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByUsername(String username);

  Optional<UserEntity> findByUsername(String username);

  List<UserEntity> findTop20ByUsernameContainingIgnoreCaseAndIdNot(String usernameFragment, String excludeId);
}
