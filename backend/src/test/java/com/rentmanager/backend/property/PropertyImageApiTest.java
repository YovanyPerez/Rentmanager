package com.rentmanager.backend.property;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.rentmanager.backend.domain.Owner;
import com.rentmanager.backend.domain.Role;
import com.rentmanager.backend.domain.User;
import com.rentmanager.backend.repository.OwnerRepository;
import com.rentmanager.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.uploads.dir=target/test-uploads")
class PropertyImageApiTest {

  private static final String PASSWORD = "secret123";
  private static final String ADMIN = "admin-img@test.local";
  private static final String OWNER = "owner-img@test.local";
  private static final String OTHER_OWNER = "owner-img-two@test.local";
  private static final String TENANT = "tenant-img@test.local";
  private static final byte[] PNG = {
      (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x00};

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository users;

  @Autowired
  private OwnerRepository owners;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Test
  void adminUploadsImageAndGalleryIsExposed() throws Exception {
    long ownerId = createOwner(OWNER);
    String token = login(ADMIN, Role.ADMIN);
    long propertyId = createProperty(token, ownerId, "Con galeria", 900.00);

    String body = mockMvc.perform(upload(propertyId, token, "foto.png", "image/png", PNG))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.url").value(startsWith("/uploads/properties/")))
        .andExpect(jsonPath("$.position").value(0))
        .andReturn().getResponse().getContentAsString();
    String url = JsonPath.read(body, "$.url");

    mockMvc.perform(get("/api/properties/{id}", propertyId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.imageUrl").value(url))
        .andExpect(jsonPath("$.images.length()").value(1));

    mockMvc.perform(get("/api/public/properties/{id}", propertyId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.imageUrl").value(url))
        .andExpect(jsonPath("$.images.length()").value(1));
  }

  @Test
  void ownerManagesImagesOfOwnProperty() throws Exception {
    long ownerId = createOwner(OWNER);
    String adminToken = login(ADMIN, Role.ADMIN);
    long propertyId = createProperty(adminToken, ownerId, "Del dueno", 900.00);
    String ownerToken = login(OWNER, Role.OWNER);

    String body = mockMvc.perform(upload(propertyId, ownerToken, "foto.png", "image/png", PNG))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    Number imageId = JsonPath.read(body, "$.id");

    mockMvc.perform(delete("/api/properties/{id}/images/{imageId}", propertyId, imageId.longValue())
            .header("Authorization", "Bearer " + ownerToken))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/properties/{id}", propertyId).header("Authorization", "Bearer " + adminToken))
        .andExpect(jsonPath("$.images.length()").value(0));
  }

  @Test
  void ownerCannotManageImagesOfForeignProperty() throws Exception {
    long otherOwnerId = createOwner(OTHER_OWNER);
    String adminToken = login(ADMIN, Role.ADMIN);
    long propertyId = createProperty(adminToken, otherOwnerId, "Ajena", 900.00);
    String ownerToken = login(OWNER, Role.OWNER);

    mockMvc.perform(upload(propertyId, ownerToken, "foto.png", "image/png", PNG))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void tenantCannotUploadImages() throws Exception {
    long ownerId = createOwner(OWNER);
    String adminToken = login(ADMIN, Role.ADMIN);
    long propertyId = createProperty(adminToken, ownerId, "Solo admin", 900.00);
    String tenantToken = login(TENANT, Role.TENANT);

    mockMvc.perform(upload(propertyId, tenantToken, "foto.png", "image/png", PNG))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void rejectsFilesThatAreNotRealImages() throws Exception {
    long ownerId = createOwner(OWNER);
    String token = login(ADMIN, Role.ADMIN);
    long propertyId = createProperty(token, ownerId, "No imagen", 900.00);

    mockMvc.perform(upload(propertyId, token, "nota.txt", "text/plain", "hola".getBytes()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_IMAGE_TYPE"));

    mockMvc.perform(upload(propertyId, token, "falsa.png", "image/png", "no soy un png".getBytes()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_IMAGE_TYPE"));
  }

  @Test
  void rejectsFilesOverFiveMegabytes() throws Exception {
    long ownerId = createOwner(OWNER);
    String token = login(ADMIN, Role.ADMIN);
    long propertyId = createProperty(token, ownerId, "Muy grande", 900.00);

    byte[] oversized = new byte[5 * 1024 * 1024 + 1];
    System.arraycopy(PNG, 0, oversized, 0, PNG.length);

    mockMvc.perform(upload(propertyId, token, "grande.png", "image/png", oversized))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
  }

  @Test
  void galleryIsLimitedToSixImages() throws Exception {
    long ownerId = createOwner(OWNER);
    String token = login(ADMIN, Role.ADMIN);
    long propertyId = createProperty(token, ownerId, "Limite", 900.00);

    for (int index = 0; index < 6; index++) {
      mockMvc.perform(upload(propertyId, token, "foto" + index + ".png", "image/png", PNG))
          .andExpect(status().isCreated());
    }

    mockMvc.perform(upload(propertyId, token, "siete.png", "image/png", PNG))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IMAGE_LIMIT_REACHED"));
  }

  @Test
  void coverAndReorderChangeTheGalleryOrder() throws Exception {
    long ownerId = createOwner(OWNER);
    String token = login(ADMIN, Role.ADMIN);
    long propertyId = createProperty(token, ownerId, "Portada", 900.00);

    Number firstId = uploadId(propertyId, token);
    Number secondId = uploadId(propertyId, token);
    Number thirdId = uploadId(propertyId, token);

    mockMvc.perform(put("/api/properties/{id}/images/{imageId}/cover", propertyId, thirdId.longValue())
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(thirdId.longValue()))
        .andExpect(jsonPath("$[1].id").value(firstId.longValue()))
        .andExpect(jsonPath("$[2].id").value(secondId.longValue()));

    mockMvc.perform(put("/api/properties/{id}/images/order", propertyId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"imageIds":[%d,%d,%d]}
                """.formatted(secondId.longValue(), firstId.longValue(), thirdId.longValue())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(secondId.longValue()))
        .andExpect(jsonPath("$[2].id").value(thirdId.longValue()));

    mockMvc.perform(get("/api/properties/{id}", propertyId).header("Authorization", "Bearer " + token))
        .andExpect(jsonPath("$.images[0].id").value(secondId.longValue()));
  }

  private Number uploadId(long propertyId, String token) throws Exception {
    String body = mockMvc.perform(upload(propertyId, token, "foto.png", "image/png", PNG))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return JsonPath.read(body, "$.id");
  }

  private RequestBuilder upload(long propertyId, String token,
      String fileName, String contentType, byte[] content) {
    return multipart("/api/properties/{id}/images", propertyId)
        .file(new MockMultipartFile("file", fileName, contentType, content))
        .header("Authorization", "Bearer " + token);
  }

  private long createProperty(String token, long ownerId, String address, double rent) throws Exception {
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
