package com.rentmanager.backend.auth;

import com.rentmanager.backend.domain.Role;
import com.rentmanager.backend.domain.User;
import com.rentmanager.backend.error.ApiException;
import com.rentmanager.backend.error.ErrorCode;
import com.rentmanager.backend.repository.UserRepository;
import com.rentmanager.backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  /** Public registration always creates a TENANT; other roles are created by an administrator. */
  @Transactional
  public AuthResponse register(RegisterRequest request) {
    if (users.existsByEmail(request.email())) {
      throw new ApiException(ErrorCode.EMAIL_ALREADY_USED);
    }
    User user = new User();
    user.setEmail(request.email());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setFullName(request.fullName());
    user.setRole(Role.TENANT);
    users.save(user);
    return AuthResponse.from(jwtService.issue(user), user);
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest request) {
    User user = users.findByEmail(request.email())
        .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));
    if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
    }
    return AuthResponse.from(jwtService.issue(user), user);
  }
}
