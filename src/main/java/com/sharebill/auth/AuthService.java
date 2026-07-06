package com.sharebill.auth;

import com.sharebill.common.AuthenticationException;
import com.sharebill.common.ConflictException;
import com.sharebill.common.IdGenerator;
import com.sharebill.user.UserDto;
import com.sharebill.user.UserEntity;
import com.sharebill.user.UserRepository;
import com.sharebill.user.UsernameGenerator;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final UsernameGenerator usernameGenerator;

  public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
      UsernameGenerator usernameGenerator) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.usernameGenerator = usernameGenerator;
  }

  public AuthResponse signup(SignupRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new ConflictException("Email already registered: " + request.email());
    }

    UserEntity user = new UserEntity(
        IdGenerator.next("user"),
        request.email(),
        passwordEncoder.encode(request.password()),
        request.displayName(),
        usernameGenerator.generateFromEmail(request.email()),
        Instant.now()
    );
    userRepository.save(user);

    return new AuthResponse(jwtService.generateToken(user.getId()), UserDto.from(user));
  }

  public AuthResponse login(LoginRequest request) {
    UserEntity user = userRepository.findByEmail(request.email())
        .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new AuthenticationException("Invalid email or password");
    }

    return new AuthResponse(jwtService.generateToken(user.getId()), UserDto.from(user));
  }
}
