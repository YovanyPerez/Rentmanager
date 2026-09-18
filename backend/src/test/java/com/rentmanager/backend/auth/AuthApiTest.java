package com.rentmanager.backend.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.rentmanager.backend.domain.Role;
import com.rentmanager.backend.domain.User;
import com.rentmanager.backend.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthApiTest {

  private static final String PASSWORD = "secret123";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository users;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtEncoder jwtEncoder;

  @Test
  void registerAlwaysCreatesTenant() throws Exception {
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"tenant@test.local","password":"secret123","fullName":"Test Tenant","role":"ADMIN"}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.role").value("TENANT"))
        .andExpect(jsonPath("$.token").isNotEmpty());
  }

  @Test
  void registerRejectsDuplicateEmail() throws Exception {
    createUser("dup@test.local", Role.TENANT);

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"dup@test.local","password":"secret123","fullName":"Other"}
                """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"));
  }

  @Test
  void registerValidatesInput() throws Exception {
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"not-an-email","password":"short","fullName":""}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors.length()").value(3));
  }

  @Test
  void malformedJsonReturnsBadRequest() throws Exception {
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{not-json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
  }

  @Test
  void loginRejectsWrongPassword() throws Exception {
    createUser("login@test.local", Role.TENANT);

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"login@test.local","password":"wrong-password"}
                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
  }

  @Test
  void expiredTokenIsRejected() throws Exception {
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("rentmanager")
        .subject("expired@test.local")
        .issuedAt(Instant.now().minusSeconds(3600))
        .expiresAt(Instant.now().minusSeconds(60))
        .claim("role", "ADMIN")
        .build();
    String token = jwtEncoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();

    mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void protectedEndpointsRequireAuthentication() throws Exception {
    mockMvc.perform(get("/api/users"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void tenantCannotListUsers() throws Exception {
    String token = registerAndGetToken("tenant2@test.local");

    mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void adminCanListUsers() throws Exception {
    createUser("admin-test@test.local", Role.ADMIN);
    String token = loginAndGetToken("admin-test@test.local");

    mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].role").isNotEmpty());
  }

  private void createUser(String email, Role role) {
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(PASSWORD));
    user.setFullName("Test " + role);
    user.setRole(role);
    users.save(user);
  }

  private String registerAndGetToken(String email) throws Exception {
    String response = mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"%s","password":"%s","fullName":"Test Tenant"}
                """.formatted(email, PASSWORD)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return JsonPath.read(response, "$.token");
  }

  private String loginAndGetToken(String email) throws Exception {
    String response = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"email":"%s","password":"%s"}
                """.formatted(email, PASSWORD)))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    return JsonPath.read(response, "$.token");
  }
}
