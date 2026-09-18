package com.rentmanager.backend.owner;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.rentmanager.backend.domain.Contract;
import com.rentmanager.backend.domain.Owner;
import com.rentmanager.backend.domain.Property;
import com.rentmanager.backend.domain.Role;
import com.rentmanager.backend.domain.Tenant;
import com.rentmanager.backend.domain.User;
import com.rentmanager.backend.repository.ContractRepository;
import com.rentmanager.backend.repository.OwnerRepository;
import com.rentmanager.backend.repository.PropertyRepository;
import com.rentmanager.backend.repository.TenantRepository;
import com.rentmanager.backend.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OwnerTenantApiTest {

  private static final String PASSWORD = "secret123";
  private static final String ADMIN = "admin-party@test.local";
  private static final String OWNER_USER = "owner-user@test.local";
  private static final String TENANT_USER = "tenant-user@test.local";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository users;

  @Autowired
  private OwnerRepository owners;

  @Autowired
  private TenantRepository tenants;

  @Autowired
  private PropertyRepository properties;

  @Autowired
  private ContractRepository contracts;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Test
  void adminCanCreateOwnerWithLinkedUser() throws Exception {
    long userId = createUser(OWNER_USER, Role.OWNER).getId();
    String token = login(ADMIN, Role.ADMIN);

    mockMvc.perform(post("/api/owners")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"fullName":"Ana Propietaria","email":"ana.p@test.local","phone":"+34 600 000 001","userId":%d}
                """.formatted(userId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value(userId))
        .andExpect(jsonPath("$.fullName").value("Ana Propietaria"));
  }

  @Test
  void createOwnerRejectsWrongUserRole() throws Exception {
    long tenantUserId = createUser(TENANT_USER, Role.TENANT).getId();
    String token = login(ADMIN, Role.ADMIN);

    mockMvc.perform(post("/api/owners")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"fullName":"Wrong role","userId":%d}
                """.formatted(tenantUserId)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("USER_ROLE_MISMATCH"));
  }

  @Test
  void createOwnerRejectsAlreadyLinkedUser() throws Exception {
    long userId = createUser(OWNER_USER, Role.OWNER).getId();
    String token = login(ADMIN, Role.ADMIN);
    String body = """
        {"fullName":"First owner","userId":%d}
        """.formatted(userId);

    mockMvc.perform(post("/api/owners").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/owners").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("USER_ALREADY_LINKED"));
  }

  @Test
  void createOwnerValidatesInput() throws Exception {
    String token = login(ADMIN, Role.ADMIN);

    mockMvc.perform(post("/api/owners")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"fullName":"","email":"not-an-email"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors.length()").value(2));
  }

  @Test
  void ownerLifecycleUpdateAndDelete() throws Exception {
    String token = login(ADMIN, Role.ADMIN);
    long ownerId = createOwner(token, "Lifecycle owner", null);

    mockMvc.perform(put("/api/owners/{id}", ownerId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"fullName":"Renamed owner","phone":"+34 600 000 002"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("Renamed owner"))
        .andExpect(jsonPath("$.userId").doesNotExist());

    mockMvc.perform(delete("/api/owners/{id}", ownerId).header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/owners/{id}", ownerId).header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("OWNER_NOT_FOUND"));
  }

  @Test
  void deleteOwnerWithPropertiesFails() throws Exception {
    String token = login(ADMIN, Role.ADMIN);
    long ownerId = createOwner(token, "With properties", null);
    Property property = new Property();
    property.setOwner(owners.findById(ownerId).orElseThrow());
    property.setAddress("Dependency 1");
    property.setCity("Madrid");
    property.setMonthlyRent(new BigDecimal("1000.00"));
    properties.save(property);

    mockMvc.perform(delete("/api/owners/{id}", ownerId).header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("OWNER_HAS_PROPERTIES"));
  }

  @Test
  void tenantLifecycleCreateUpdateDelete() throws Exception {
    long userId = createUser(TENANT_USER, Role.TENANT).getId();
    String token = login(ADMIN, Role.ADMIN);

    String response = mockMvc.perform(post("/api/tenants")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"fullName":"Tomás Inquilino","userId":%d}
                """.formatted(userId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value(userId))
        .andReturn().getResponse().getContentAsString();
    long tenantId = ((Number) JsonPath.read(response, "$.id")).longValue();

    mockMvc.perform(put("/api/tenants/{id}", tenantId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"fullName":"Tomás Actualizado","phone":"+34 600 000 003"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("Tomás Actualizado"));

    mockMvc.perform(delete("/api/tenants/{id}", tenantId).header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
  }

  @Test
  void deleteTenantWithContractsFails() throws Exception {
    String token = login(ADMIN, Role.ADMIN);
    long ownerId = createOwner(token, "Contract owner", null);
    long tenantId = createTenant(token, "Contract tenant");
    Property property = new Property();
    property.setOwner(owners.findById(ownerId).orElseThrow());
    property.setAddress("Contract 1");
    property.setCity("Madrid");
    property.setMonthlyRent(new BigDecimal("950.00"));
    properties.save(property);
    Contract contract = new Contract();
    contract.setProperty(property);
    contract.setTenant(tenants.findById(tenantId).orElseThrow());
    contract.setStartDate(LocalDate.now().plusDays(1));
    contract.setEndDate(LocalDate.now().plusMonths(12));
    contract.setMonthlyRent(new BigDecimal("950.00"));
    contracts.save(contract);

    mockMvc.perform(delete("/api/tenants/{id}", tenantId).header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TENANT_HAS_CONTRACTS"));
  }

  @Test
  void nonAdminCannotManageOwners() throws Exception {
    createUser(OWNER_USER, Role.OWNER);
    createUser(TENANT_USER, Role.TENANT);
    String ownerToken = login(OWNER_USER, Role.OWNER);
    String tenantToken = login(TENANT_USER, Role.TENANT);

    mockMvc.perform(get("/api/owners").header("Authorization", "Bearer " + ownerToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));

    mockMvc.perform(get("/api/owners").header("Authorization", "Bearer " + tenantToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  private long createOwner(String token, String fullName, Long userId) throws Exception {
    String userPart = userId == null ? "" : ",\"userId\":%d".formatted(userId);
    String response = mockMvc.perform(post("/api/owners")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"fullName":"%s"%s}
                """.formatted(fullName, userPart)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(response, "$.id")).longValue();
  }

  private long createTenant(String token, String fullName) throws Exception {
    String response = mockMvc.perform(post("/api/tenants")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"fullName":"%s"}
                """.formatted(fullName)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(response, "$.id")).longValue();
  }

  private User createUser(String email, Role role) {
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(PASSWORD));
    user.setFullName(role + " " + email);
    user.setRole(role);
    return users.save(user);
  }

  private String login(String email, Role role) throws Exception {
    if (users.findByEmail(email).isEmpty()) {
      createUser(email, role);
    }
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
