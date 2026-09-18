package com.rentmanager.backend.web;

import com.rentmanager.backend.domain.User;
import com.rentmanager.backend.repository.UserRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ADMIN-only user management (see AGENTS.md section 6). */
@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserRepository users;

  public UserController(UserRepository users) {
    this.users = users;
  }

  @GetMapping
  public List<UserResponse> list() {
    return users.findAll().stream().map(UserResponse::from).toList();
  }

  public record UserResponse(Long id, String email, String fullName, String role, boolean active) {

    static UserResponse from(User user) {
      return new UserResponse(user.getId(), user.getEmail(), user.getFullName(),
          user.getRole().name(), user.isActive());
    }
  }
}
