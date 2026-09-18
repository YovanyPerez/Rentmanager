package com.rentmanager.backend.security;

import com.rentmanager.backend.domain.User;
import com.rentmanager.backend.error.ApiException;
import com.rentmanager.backend.error.ErrorCode;
import com.rentmanager.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resolves the authenticated user from the JWT subject (email). */
@Component
public class CurrentUser {

  private final UserRepository users;

  public CurrentUser(UserRepository users) {
    this.users = users;
  }

  public User get() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new ApiException(ErrorCode.UNAUTHORIZED);
    }
    return users.findByEmail(authentication.getName())
        .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
  }
}
