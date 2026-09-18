package com.rentmanager.backend.maintenance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.rentmanager.backend.domain.Contract;
import com.rentmanager.backend.domain.ContractStatus;
import com.rentmanager.backend.domain.MaintenanceRequest;
import com.rentmanager.backend.domain.MaintenanceStatus;
import com.rentmanager.backend.domain.Owner;
import com.rentmanager.backend.domain.Property;
import com.rentmanager.backend.domain.PropertyStatus;
import com.rentmanager.backend.domain.Role;
import com.rentmanager.backend.domain.Tenant;
import com.rentmanager.backend.domain.User;
import com.rentmanager.backend.repository.ContractRepository;
import com.rentmanager.backend.repository.MaintenanceRequestRepository;
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
class MaintenanceApiTest {

  private static final String PASSWORD = "secret123";
  private static final String ADMIN = "admin-maint@test.local";
  private static final String OWNER_USER = "owner-maint@test.local";
  private static final String OTHER_OWNER_USER = "owner-maint-2@test.local";
  private static final String TENANT_USER = "tenant-maint@test.local";
  private static final String OTHER_TENANT_USER = "tenant-maint-2@test.local";

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
  private MaintenanceRequestRepository requests;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Test
  void tenantCreatesRequestOnRentedProperty() throws Exception {
    Property property = newProperty(newOwner(OWNER_USER));
    Tenant tenant = newTenant(TENANT_USER);
    activeContract(property, tenant);
    String token = login(TENANT_USER, Role.TENANT);

    mockMvc.perform(post("/api/maintenance")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"propertyId":%d,"title":"Fuga de agua","description":"Cocina"}
                """.formatted(property.getId())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("OPEN"))
        .andExpect(jsonPath("$.createdById").value(tenant.getUser().getId()))
        .andExpect(jsonPath("$.propertyId").value(property.getId()));
  }

  @Test
  void tenantCannotCreateOnNonRentedProperty() throws Exception {
    Property property = newProperty(newOwner(OWNER_USER));
    Tenant tenant = newTenant(TENANT_USER);
    Contract draft = new Contract();
    draft.setProperty(property);
    draft.setTenant(tenant);
    draft.setStartDate(LocalDate.of(2030, 1, 1));
    draft.setEndDate(LocalDate.of(2030, 12, 31));
    draft.setMonthlyRent(new BigDecimal("1000.00"));
    draft.setStatus(ContractStatus.DRAFT);
    contracts.save(draft);
    String token = login(TENANT_USER, Role.TENANT);

    mockMvc.perform(post("/api/maintenance")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"propertyId":%d,"title":"No alquilada"}
                """.formatted(property.getId())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void adminCanCreateOnAnyProperty() throws Exception {
    Property property = newProperty(newOwner(OWNER_USER));
    String token = login(ADMIN, Role.ADMIN);

    mockMvc.perform(post("/api/maintenance")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"propertyId":%d,"title":"Revision anual"}
                """.formatted(property.getId())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("OPEN"));
  }

  @Test
  void createValidatesInput() throws Exception {
    String token = login(ADMIN, Role.ADMIN);

    mockMvc.perform(post("/api/maintenance")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"propertyId":1,"title":""}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void tenantSeesOnlyOwnRequests() throws Exception {
    Property property = newProperty(newOwner(OWNER_USER));
    User tenantUser = createUser(TENANT_USER, Role.TENANT);
    User otherTenantUser = createUser(OTHER_TENANT_USER, Role.TENANT);
    newRequest(property, tenantUser, "Mine");
    long foreignId = newRequest(property, otherTenantUser, "Not mine").getId();
    String token = login(TENANT_USER, Role.TENANT);

    mockMvc.perform(get("/api/maintenance").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].title").value("Mine"));

    mockMvc.perform(get("/api/maintenance/{id}", foreignId).header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void ownerSeesRequestsOnOwnProperties() throws Exception {
    Property mine = newProperty(newOwner(OWNER_USER));
    Property notMine = newProperty(newOwner(OTHER_OWNER_USER));
    User tenantUser = createUser(TENANT_USER, Role.TENANT);
    newRequest(mine, tenantUser, "On my property");
    long foreignId = newRequest(notMine, tenantUser, "On other property").getId();
    String token = login(OWNER_USER, Role.OWNER);

    mockMvc.perform(get("/api/maintenance").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].propertyId").value(mine.getId()));

    mockMvc.perform(get("/api/maintenance/{id}", foreignId).header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void adminUpdatesStatusAndAssignment() throws Exception {
    Property property = newProperty(newOwner(OWNER_USER));
    long requestId = newRequest(property, createUser(TENANT_USER, Role.TENANT), "Reparar").getId();
    String token = login(ADMIN, Role.ADMIN);

    mockMvc.perform(put("/api/maintenance/{id}", requestId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"IN_PROGRESS","assignedTo":"Fontanero Pérez"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.assignedTo").value("Fontanero Pérez"));

    mockMvc.perform(put("/api/maintenance/{id}", requestId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"COMPLETED"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.assignedTo").value("Fontanero Pérez"));

    mockMvc.perform(put("/api/maintenance/{id}", requestId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"IN_PROGRESS"}
                """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
  }

  @Test
  void openCannotBeCompletedDirectly() throws Exception {
    Property property = newProperty(newOwner(OWNER_USER));
    long requestId = newRequest(property, createUser(TENANT_USER, Role.TENANT), "Reparar").getId();
    String token = login(ADMIN, Role.ADMIN);

    mockMvc.perform(put("/api/maintenance/{id}", requestId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"COMPLETED"}
                """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
  }

  @Test
  void ownerCannotCreateOrUpdate() throws Exception {
    Property property = newProperty(newOwner(OWNER_USER));
    long requestId = newRequest(property, createUser(TENANT_USER, Role.TENANT), "Reparar").getId();
    String token = login(OWNER_USER, Role.OWNER);

    mockMvc.perform(post("/api/maintenance")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"propertyId":%d,"title":"No permitido"}
                """.formatted(property.getId())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));

    mockMvc.perform(put("/api/maintenance/{id}", requestId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"IN_PROGRESS"}
                """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  private MaintenanceRequest newRequest(Property property, User createdBy, String title) {
    MaintenanceRequest request = new MaintenanceRequest();
    request.setProperty(property);
    request.setCreatedBy(createdBy);
    request.setTitle(title);
    request.setStatus(MaintenanceStatus.OPEN);
    return requests.save(request);
  }

  private void activeContract(Property property, Tenant tenant) {
    Contract contract = new Contract();
    contract.setProperty(property);
    contract.setTenant(tenant);
    contract.setStartDate(LocalDate.of(2030, 1, 1));
    contract.setEndDate(LocalDate.of(2030, 12, 31));
    contract.setMonthlyRent(new BigDecimal("1000.00"));
    contract.setStatus(ContractStatus.ACTIVE);
    property.setStatus(PropertyStatus.RENTED);
    contracts.save(contract);
  }

  private Owner newOwner(String email) {
    User user = createUser(email, Role.OWNER);
    Owner owner = new Owner();
    owner.setUser(user);
    owner.setFullName("Owner " + email);
    return owners.save(owner);
  }

  private Tenant newTenant(String email) {
    User user = createUser(email, Role.TENANT);
    Tenant tenant = new Tenant();
    tenant.setUser(user);
    tenant.setFullName("Tenant " + email);
    return tenants.save(tenant);
  }

  private Property newProperty(Owner owner) {
    Property property = new Property();
    property.setOwner(owner);
    property.setAddress("Maintenance test " + System.nanoTime());
    property.setCity("Madrid");
    property.setMonthlyRent(new BigDecimal("1000.00"));
    return properties.save(property);
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
