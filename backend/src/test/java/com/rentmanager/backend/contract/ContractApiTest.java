package com.rentmanager.backend.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.rentmanager.backend.domain.Contract;
import com.rentmanager.backend.domain.ContractStatus;
import com.rentmanager.backend.domain.Owner;
import com.rentmanager.backend.domain.Property;
import com.rentmanager.backend.domain.PropertyStatus;
import com.rentmanager.backend.domain.Role;
import com.rentmanager.backend.domain.Tenant;
import com.rentmanager.backend.domain.User;
import com.rentmanager.backend.repository.ContractRepository;
import com.rentmanager.backend.repository.OwnerRepository;
import com.rentmanager.backend.repository.PaymentRepository;
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
class ContractApiTest {

  private static final String PASSWORD = "secret123";
  private static final String ADMIN = "admin-contract@test.local";
  private static final String OWNER_USER = "owner-contract@test.local";
  private static final String OTHER_OWNER_USER = "owner-contract-2@test.local";
  private static final String TENANT_USER = "tenant-contract@test.local";
  private static final String OTHER_TENANT_USER = "tenant-contract-2@test.local";

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
  private PaymentRepository payments;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Test
  void adminCreatesDraftContract() throws Exception {
    String token = login(ADMIN, Role.ADMIN);
    Property property = newProperty(newOwner(OWNER_USER));
    Tenant tenant = newTenant(TENANT_USER);

    mockMvc.perform(post("/api/contracts")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(contractBody(property, tenant, "2030-01-01", "2030-12-31", "1200.00")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.propertyId").value(property.getId()))
        .andExpect(jsonPath("$.tenantId").value(tenant.getId()));
  }

  @Test
  void createValidatesDateRange() throws Exception {
    String token = login(ADMIN, Role.ADMIN);
    Property property = newProperty(newOwner(OWNER_USER));
    Tenant tenant = newTenant(TENANT_USER);

    mockMvc.perform(post("/api/contracts")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(contractBody(property, tenant, "2030-12-31", "2030-01-01", "1200.00")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
  }

  @Test
  void createRejectsUnknownPropertyAndTenant() throws Exception {
    String token = login(ADMIN, Role.ADMIN);

    mockMvc.perform(post("/api/contracts")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"propertyId":999999,"tenantId":1,"startDate":"2030-01-01","endDate":"2030-12-31","monthlyRent":100.00}
                """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PROPERTY_NOT_FOUND"));

    mockMvc.perform(post("/api/contracts")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"propertyId":1,"tenantId":999999,"startDate":"2030-01-01","endDate":"2030-12-31","monthlyRent":100.00}
                """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TENANT_NOT_FOUND"));
  }

  @Test
  void activateMarksPropertyRentedAndGeneratesInstallments() throws Exception {
    String token = login(ADMIN, Role.ADMIN);
    Property property = newProperty(newOwner(OWNER_USER));
    Tenant tenant = newTenant(TENANT_USER);
    long contractId = createContract(token, property, tenant, "2030-01-01", "2030-12-31", "1200.00");

    mockMvc.perform(post("/api/contracts/{id}/activate", contractId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    assertThat(property.getStatus()).isEqualTo(PropertyStatus.RENTED);
    assertThat(payments.countByContractId(contractId)).isEqualTo(12);
  }

  @Test
  void activateRejectsSecondActiveContract() throws Exception {
    String token = login(ADMIN, Role.ADMIN);
    Property property = newProperty(newOwner(OWNER_USER));
    Tenant tenant = newTenant(TENANT_USER);
    long first = createContract(token, property, tenant, "2030-01-01", "2030-12-31", "1200.00");
    activate(token, first);

    long second = createContract(token, property, tenant, "2031-01-01", "2031-12-31", "1200.00");
    mockMvc.perform(post("/api/contracts/{id}/activate", second)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT_ACTIVE_CONTRACT"));
  }

  @Test
  void activateRejectsNonAvailableProperty() throws Exception {
    String token = login(ADMIN, Role.ADMIN);
    Property property = newProperty(newOwner(OWNER_USER));
    property.setStatus(PropertyStatus.INACTIVE);
    Tenant tenant = newTenant(TENANT_USER);
    long contractId = createContract(token, property, tenant, "2030-01-01", "2030-12-31", "1200.00");

    mockMvc.perform(post("/api/contracts/{id}/activate", contractId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
  }

  @Test
  void terminateReleasesProperty() throws Exception {
    String token = login(ADMIN, Role.ADMIN);
    Property property = newProperty(newOwner(OWNER_USER));
    Tenant tenant = newTenant(TENANT_USER);
    long contractId = createContract(token, property, tenant, "2030-01-01", "2030-12-31", "1200.00");
    activate(token, contractId);

    mockMvc.perform(post("/api/contracts/{id}/terminate", contractId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("TERMINATED"));

    assertThat(property.getStatus()).isEqualTo(PropertyStatus.AVAILABLE);
    assertThat(payments.countByContractId(contractId)).isEqualTo(12);
  }

  @Test
  void expiredContractIsEvaluatedOnRead() throws Exception {
    String token = login(ADMIN, Role.ADMIN);
    Property property = newProperty(newOwner(OWNER_USER));
    Tenant tenant = newTenant(TENANT_USER);
    Contract contract = new Contract();
    contract.setProperty(property);
    contract.setTenant(tenant);
    contract.setStartDate(LocalDate.now().minusMonths(13));
    contract.setEndDate(LocalDate.now().minusDays(1));
    contract.setMonthlyRent(new BigDecimal("1000.00"));
    contract.setStatus(ContractStatus.ACTIVE);
    property.setStatus(PropertyStatus.RENTED);
    contracts.save(contract);

    mockMvc.perform(get("/api/contracts/{id}", contract.getId())
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("EXPIRED"));

    assertThat(property.getStatus()).isEqualTo(PropertyStatus.AVAILABLE);
  }

  @Test
  void deleteAndUpdateOnlyAllowedForDraft() throws Exception {
    String token = login(ADMIN, Role.ADMIN);
    Property property = newProperty(newOwner(OWNER_USER));
    Tenant tenant = newTenant(TENANT_USER);
    long contractId = createContract(token, property, tenant, "2030-01-01", "2030-12-31", "1200.00");
    activate(token, contractId);

    mockMvc.perform(delete("/api/contracts/{id}", contractId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));

    mockMvc.perform(put("/api/contracts/{id}", contractId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(contractBody(property, tenant, "2030-01-01", "2030-12-31", "999.00")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));

    long draftId = createContract(token, newProperty(newOwner(OTHER_OWNER_USER)),
        newTenant(OTHER_TENANT_USER), "2032-01-01", "2032-12-31", "1000.00");
    mockMvc.perform(delete("/api/contracts/{id}", draftId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
  }

  @Test
  void ownerSeesOnlyContractsOfOwnProperties() throws Exception {
    String adminToken = login(ADMIN, Role.ADMIN);
    Owner owner = newOwner(OWNER_USER);
    Property mine = newProperty(owner);
    Property notMine = newProperty(newOwner(OTHER_OWNER_USER));
    Tenant tenant = newTenant(TENANT_USER);
    createContract(adminToken, mine, tenant, "2030-01-01", "2030-12-31", "1200.00");
    long foreignContractId = createContract(adminToken, notMine, tenant, "2030-01-01", "2030-12-31", "1300.00");
    String token = login(OWNER_USER, Role.OWNER);

    mockMvc.perform(get("/api/contracts").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].propertyId").value(mine.getId()));

    mockMvc.perform(get("/api/contracts/{id}", foreignContractId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void tenantSeesOnlyOwnContracts() throws Exception {
    String adminToken = login(ADMIN, Role.ADMIN);
    Tenant mine = newTenant(TENANT_USER);
    Tenant other = newTenant(OTHER_TENANT_USER);
    Property property = newProperty(newOwner(OWNER_USER));
    createContract(adminToken, property, mine, "2030-01-01", "2030-12-31", "1200.00");
    long foreignContractId = createContract(adminToken, property, other, "2030-01-01", "2030-12-31", "1200.00");
    String token = login(TENANT_USER, Role.TENANT);

    mockMvc.perform(get("/api/contracts").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].tenantId").value(mine.getId()));

    mockMvc.perform(get("/api/contracts/{id}", foreignContractId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void nonAdminCannotMutateContracts() throws Exception {
    String adminToken = login(ADMIN, Role.ADMIN);
    Property property = newProperty(newOwner(OWNER_USER));
    Tenant tenant = newTenant(TENANT_USER);
    long contractId = createContract(adminToken, property, tenant, "2030-01-01", "2030-12-31", "1200.00");
    String ownerToken = login(OWNER_USER, Role.OWNER);

    mockMvc.perform(post("/api/contracts/{id}/activate", contractId)
            .header("Authorization", "Bearer " + ownerToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));

    mockMvc.perform(post("/api/contracts")
            .header("Authorization", "Bearer " + ownerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(contractBody(property, tenant, "2030-01-01", "2030-12-31", "1200.00")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  private void activate(String token, long contractId) throws Exception {
    mockMvc.perform(post("/api/contracts/{id}/activate", contractId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  private long createContract(String token, Property property, Tenant tenant,
      String start, String end, String rent) throws Exception {
    String response = mockMvc.perform(post("/api/contracts")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(contractBody(property, tenant, start, end, rent)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(response, "$.id")).longValue();
  }

  private static String contractBody(Property property, Tenant tenant,
      String start, String end, String rent) {
    return """
        {"propertyId":%d,"tenantId":%d,"startDate":"%s","endDate":"%s","monthlyRent":%s}
        """.formatted(property.getId(), tenant.getId(), start, end, rent);
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
    property.setAddress("Contract test " + System.nanoTime());
    property.setCity("Madrid");
    property.setMonthlyRent(new BigDecimal("1200.00"));
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
