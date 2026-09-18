package com.rentmanager.backend.config;

import com.rentmanager.backend.domain.Role;
import com.rentmanager.backend.domain.User;
import com.rentmanager.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the initial ADMIN account when the database has none. Development convenience. */
@Component
public class DevAdminSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(DevAdminSeeder.class);

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final boolean enabled;
  private final String email;
  private final String password;

  public DevAdminSeeder(UserRepository users, PasswordEncoder passwordEncoder,
      @Value("${app.admin.enabled}") boolean enabled,
      @Value("${app.admin.email}") String email,
      @Value("${app.admin.password}") String password) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.enabled = enabled;
    this.email = email;
    this.password = password;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!enabled || users.existsByEmail(email)) {
      return;
    }
    User admin = new User();
    admin.setEmail(email);
    admin.setPasswordHash(passwordEncoder.encode(password));
    admin.setFullName("Administrator");
    admin.setRole(Role.ADMIN);
    users.save(admin);
    log.warn("Seeded initial ADMIN account '{}'. Change ADMIN_PASSWORD (or disable ADMIN_SEED_ENABLED) outside development.",
        email);
  }
}
