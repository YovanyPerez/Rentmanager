package com.rentmanager.backend.property;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.rentmanager.backend.domain.Owner;
import com.rentmanager.backend.domain.Property;
import com.rentmanager.backend.domain.PropertyStatus;
import com.rentmanager.backend.domain.Role;
import com.rentmanager.backend.domain.User;
import com.rentmanager.backend.repository.OwnerRepository;
import com.rentmanager.backend.repository.PropertyRepository;
import com.rentmanager.backend.repository.UserRepository;
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
class PropertyApiTest {

  private static final String PASSWORD = "secret123";
  private static final String ADMIN = "admin-prop@test.local";
  private static final String OWNER = "owner-prop@test.local";
  private static final String OTHER_OWNER = "owner-two@test.local";
  private static final String TENANT = "tenant-prop@test.local";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository users;

  @Autowired
  private OwnerRepository owners;

  @Autowired
  private PropertyRepository properties;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Test
  void adminCanCreateAndReadProperty() throws Exception {
    long ownerId = createOwner(OWNER);
    String token = login(ADMIN, Role.ADMIN);

    long propertyId = createProperty(token, ownerId, "Calle Mayor 1", "1200.00");

    mockMvc.perform(get("/api/properties/{id}", propertyId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.address").value("Calle Mayor 1"))
        .andExpect(jsonPath("$.status").value("AVAILABLE"))
        .andExpect(jsonPath("$.ownerId").value(ownerId));
  }

  @Test
  void createRejectsUnknownOwner() throws Exception {
    String token = login(ADMIN, Role.ADMIN);

    mockMvc.perform(post("/api/properties")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"ownerId":999999,"address":"X","city":"Y","monthlyRent":100.00}
                """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("OWNER_NOT_FOUND"));
  }

  @Test
  void createValidatesInput() throws Exception {
    String token = login(ADMIN, Role.ADMIN);
    long ownerId = createOwner(OWNER);

    mockMvc.perform(post("/api/properties")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"ownerId":%d,"address":"","city":"Y","monthlyRent":0}
                """.formatted(ownerId)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors.length()").value(2));
  }

  @Test
  void ownerListsOnlyOwnProperties() throws Exception {
    long ownerId = createOwner(OWNER);
    long otherOwnerId = createOwner(OTHER_OWNER);
    String adminToken = login(ADMIN, Role.ADMIN);
    createProperty(adminToken, ownerId, "Mine 1", "1000.00");
    createProperty(adminToken, otherOwnerId, "Not mine 1", "1000.00");
    String ownerToken = login(OWNER, Role.OWNER);

    mockMvc.perform(get("/api/properties").header("Authorization", "Bearer " + ownerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].address").value("Mine 1"));
  }

  @Test
  void ownerCannotCreateProperty() throws Exception {
    long ownerId = createOwner(OWNER);
    String token = login(OWNER, Role.OWNER);

    mockMvc.perform(post("/api/properties")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"ownerId":%d,"address":"X","city":"Y","monthlyRent":100.00}
                """.formatted(ownerId)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void ownerCannotReadAnotherOwnersProperty() throws Exception {
    long otherOwnerId = createOwner(OTHER_OWNER);
    String adminToken = login(ADMIN, Role.ADMIN);
    long propertyId = createProperty(adminToken, otherOwnerId, "Not mine 2", "1000.00");
    String token = login(OWNER, Role.OWNER);

    mockMvc.perform(get("/api/properties/{id}", propertyId).header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void tenantCannotReadProperties() throws Exception {
    String token = login(TENANT, Role.TENANT);

    mockMvc.perform(get("/api/properties").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void statusTransitionsAreValidated() throws Exception {
    long ownerId = createOwner(OWNER);
    String token = login(ADMIN, Role.ADMIN);
    long propertyId = createProperty(token, ownerId, "Transition St 1", "900.00");

    patchStatus(token, propertyId, "MAINTENANCE", 200);
    patchStatus(token, propertyId, "INACTIVE", 409);
    patchStatus(token, propertyId, "AVAILABLE", 200);
    patchStatus(token, propertyId, "RENTED", 409);
  }

  @Test
  void updateChangesData() throws Exception {
    long ownerId = createOwner(OWNER);
    long otherOwnerId = createOwner(OTHER_OWNER);
    String token = login(ADMIN, Role.ADMIN);
    long propertyId = createProperty(token, ownerId, "Old address", "900.00");

    mockMvc.perform(put("/api/properties/{id}", propertyId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"ownerId":%d,"address":"New address","city":"Bilbao","monthlyRent":1050.00}
                """.formatted(otherOwnerId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.address").value("New address"))
        .andExpect(jsonPath("$.ownerId").value(otherOwnerId))
        .andExpect(jsonPath("$.monthlyRent").value(1050.00));
  }

  @Test
  void deleteDeactivatesProperty() throws Exception {
    long ownerId = createOwner(OWNER);
    String token = login(ADMIN, Role.ADMIN);
    long propertyId = createProperty(token, ownerId, "To deactivate", "900.00");

    mockMvc.perform(delete("/api/properties/{id}", propertyId).header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/properties/{id}", propertyId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INACTIVE"));
  }

  @Test
  void rentedPropertyCannotBeDeactivated() throws Exception {
    long ownerId = createOwner(OWNER);
    String token = login(ADMIN, Role.ADMIN);
    long propertyId = createProperty(token, ownerId, "Rented 1", "900.00");
    Property property = properties.findById(propertyId).orElseThrow();
    property.setStatus(PropertyStatus.RENTED);

    mockMvc.perform(delete("/api/properties/{id}", propertyId).header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
  }

  private void patchStatus(String token, long propertyId, String status, int expected) throws Exception {
    mockMvc.perform(patch("/api/properties/{id}/status", propertyId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"%s"}
                """.formatted(status)))
        .andExpect(status().is(expected));
  }

  private long createProperty(String token, long ownerId, String address, String rent) throws Exception {
    String response = mockMvc.perform(post("/api/properties")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"ownerId":%d,"address":"%s","city":"Madrid","monthlyRent":%s}
                """.formatted(ownerId, address, rent)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(response, "$.id")).longValue();
  }

  private long createOwner(String email) {
    User user = createUser(email, Role.OWNER);
    Owner owner = new Owner();
    owner.setUser(user);
    owner.setFullName("Owner " + email);
    owner.setEmail(email);
    return owners.save(owner).getId();
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
