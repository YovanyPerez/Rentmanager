package com.rentmanager.backend.auth;

import com.rentmanager.backend.domain.User;
import com.rentmanager.backend.security.JwtService.IssuedToken;
import java.time.Instant;

public record AuthResponse(
    String token,
    Instant expiresAt,
    Long userId,
    String email,
    String fullName,
    String role) {

  public static AuthResponse from(IssuedToken issued, User user) {
    return new AuthResponse(issued.token(), issued.expiresAt(), user.getId(), user.getEmail(),
        user.getFullName(), user.getRole().name());
  }
}
