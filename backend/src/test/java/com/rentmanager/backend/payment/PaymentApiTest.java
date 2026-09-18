package com.rentmanager.backend.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.rentmanager.backend.domain.Contract;
import com.rentmanager.backend.domain.ContractStatus;
import com.rentmanager.backend.domain.Owner;
import com.rentmanager.backend.domain.Payment;
import com.rentmanager.backend.domain.PaymentStatus;
import com.rentmanager.backend.domain.Property;
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
import java.util.List;
import java.util.Map;
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
class PaymentApiTest {

  private static final String PASSWORD = "secret123";
  private static final String ADMIN = "admin-payment@test.local";
  private static final String OWNER_USER = "owner-payment@test.local";
  private static final String OTHER_OWNER_USER = "owner-payment-2@test.local";
  private static final String TENANT_USER = "tenant-payment@test.local";
  private static final String OTHER_TENANT_USER = "tenant-payment-2@test.local";

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
  void activationInstallmentsAreVisibleAsPendingPayments() throws Exception {
    String token = login(ADMIN, Role.ADMIN);
    Contract contract = newContract(newProperty(newOwner(OWNER_USER)), newTenant(TENANT_USER), ContractStatus.DRAFT);
    contract.setStartDate(LocalDate.of(2030, 1, 1));
    contract.setEndDate(LocalDate.of(2030, 3, 31));

    mockMvc.perform(post("/api/contracts/{id}/activate", contract.getId())
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    String body = mockMvc.perform(get("/api/payments").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    List<Map<String, Object>> ownPayments = JsonPath.<List<Map<String, Object>>>read(body, "$").stream()
        .filter(payment -> ((Number) payment.get("contractId")).longValue() == contract.getId())
        .toList();

    assertThat(ownPayments).hasSize(3);
    assertThat(ownPayments).allMatch(payment -> "PENDING".equals(payment.get("status")));
  }

  @Test
  void adminCreatesManualPayment() throws Exception {
    String token = login(ADMIN, Role.ADMIN);
    Contract contract = newContract(newProperty(newOwner(OWNER_USER)), newTenant(TENANT_USER), ContractStatus.DRAFT);

    mockMvc.perform(post("/api/payments")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"contractId":%d,"amount":150.00,"dueDate":"2030-01-15"}
                """.formatted(contract.getId())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.paidDate").doesNotExist());
  }

  @Test
  void createRejectsUnknownContract() throws Exception {
    String token = login(ADMIN, Role.ADMIN);

    mockMvc.perform(post("/api/payments")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"contractId":999999,"amount":150.00,"dueDate":"2030-01-15"}
                """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("CONTRACT_NOT_FOUND"));
  }

  @Test
  void createRejectsDuplicatePayment() throws Exception {
    String token = login(ADMIN, Role.ADMIN);
    Contract contract = newContract(newProperty(newOwner(OWNER_USER)), newTenant(TENANT_USER), ContractStatus.DRAFT);
    createPayment(token, contract.getId(), "150.00", "2030-01-15");

    mockMvc.perform(post("/api/payments")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"contractId":%d,"amount":150.00,"dueDate":"2030-01-15"}
                """.formatted(contract.getId())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("PAYMENT_ALREADY_EXISTS"));
  }

  @Test
  void createValidatesInput() throws Exception {
    String token = login(ADMIN, Role.ADMIN);
    Contract contract = newContract(newProperty(newOwner(OWNER_USER)), newTenant(TENANT_USER), ContractStatus.DRAFT);

    mockMvc.perform(post("/api/payments")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"contractId":%d,"amount":0,"dueDate":"2030-01-15"}
                """.formatted(contract.getId())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void markPaidRecordsPaidDate() throws Exception {
    String token = login(ADMIN, Role.ADMIN);
    Contract contract = newContract(newProperty(newOwner(OWNER_USER)), newTenant(TENANT_USER), ContractStatus.DRAFT);
    long paymentId = createPayment(token, contract.getId(), "150.00", "2030-01-15");

    mockMvc.perform(put("/api/payments/{id}", paymentId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"PAID","paidDate":"2030-01-20"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAID"))
        .andExpect(jsonPath("$.paidDate").value("2030-01-20"));

    Payment payment = payments.findById(paymentId).orElseThrow();
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
  }

  @Test
  void overdueIsEvaluatedOnReadAndCanBePaid() throws Exception {
    String token = login(ADMIN, Role.ADMIN);
    Contract contract = newContract(newProperty(newOwner(OWNER_USER)), newTenant(TENANT_USER), ContractStatus.DRAFT);
    Payment payment = new Payment();
    payment.setContract(contract);
    payment.setAmount(new BigDecimal("150.00"));
    payment.setDueDate(LocalDate.now().minusDays(3));
    payment.setStatus(PaymentStatus.PENDING);
    payments.save(payment);

    mockMvc.perform(get("/api/payments/{id}", payment.getId())
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("OVERDUE"));

    mockMvc.perform(put("/api/payments/{id}", payment.getId())
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"PAID"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAID"))
        .andExpect(jsonPath("$.paidDate").value(LocalDate.now().toString()));
  }

  @Test
  void cancelledIsFinalButIdempotent() throws Exception {
    String token = login(ADMIN, Role.ADMIN);
    Contract contract = newContract(newProperty(newOwner(OWNER_USER)), newTenant(TENANT_USER), ContractStatus.DRAFT);
    long paymentId = createPayment(token, contract.getId(), "150.00", "2030-01-15");

    mockMvc.perform(put("/api/payments/{id}", paymentId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"CANCELLED"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));

    mockMvc.perform(put("/api/payments/{id}", paymentId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"PAID"}
                """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));

    mockMvc.perform(put("/api/payments/{id}", paymentId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"CANCELLED"}
                """))
        .andExpect(status().isOk());
  }

  @Test
  void tenantSeesOnlyOwnPayments() throws Exception {
    String adminToken = login(ADMIN, Role.ADMIN);
    Tenant mine = newTenant(TENANT_USER);
    Tenant other = newTenant(OTHER_TENANT_USER);
    Property property = newProperty(newOwner(OWNER_USER));
    Contract myContract = newContract(property, mine, ContractStatus.DRAFT);
    Contract otherContract = newContract(property, other, ContractStatus.DRAFT);
    createPayment(adminToken, myContract.getId(), "100.00", "2030-02-01");
    long foreignPaymentId = createPayment(adminToken, otherContract.getId(), "100.00", "2030-02-01");
    String token = login(TENANT_USER, Role.TENANT);

    mockMvc.perform(get("/api/payments").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].contractId").value(myContract.getId()));

    mockMvc.perform(get("/api/payments/{id}", foreignPaymentId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void ownerSeesPaymentsOfOwnProperties() throws Exception {
    String adminToken = login(ADMIN, Role.ADMIN);
    Property mine = newProperty(newOwner(OWNER_USER));
    Property notMine = newProperty(newOwner(OTHER_OWNER_USER));
    Tenant tenant = newTenant(TENANT_USER);
    createPayment(adminToken, newContract(mine, tenant, ContractStatus.DRAFT).getId(), "100.00", "2030-02-01");
    long foreignPaymentId = createPayment(adminToken,
        newContract(notMine, tenant, ContractStatus.DRAFT).getId(), "100.00", "2030-02-01");
    String token = login(OWNER_USER, Role.OWNER);

    mockMvc.perform(get("/api/payments").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].propertyId").value(mine.getId()));

    mockMvc.perform(get("/api/payments/{id}", foreignPaymentId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void nonAdminCannotCreateOrUpdatePayments() throws Exception {
    String adminToken = login(ADMIN, Role.ADMIN);
    Contract contract = newContract(newProperty(newOwner(OWNER_USER)), newTenant(TENANT_USER), ContractStatus.DRAFT);
    long paymentId = createPayment(adminToken, contract.getId(), "100.00", "2030-02-01");
    String token = login(TENANT_USER, Role.TENANT);

    mockMvc.perform(post("/api/payments")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"contractId":%d,"amount":100.00,"dueDate":"2030-03-01"}
                """.formatted(contract.getId())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));

    mockMvc.perform(put("/api/payments/{id}", paymentId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"status":"PAID"}
                """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  private long createPayment(String token, long contractId, String amount, String dueDate) throws Exception {
    String response = mockMvc.perform(post("/api/payments")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"contractId":%d,"amount":%s,"dueDate":"%s"}
                """.formatted(contractId, amount, dueDate)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(response, "$.id")).longValue();
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
    property.setAddress("Payment test " + System.nanoTime());
    property.setCity("Madrid");
    property.setMonthlyRent(new BigDecimal("1000.00"));
    return properties.save(property);
  }

  private Contract newContract(Property property, Tenant tenant, ContractStatus status) {
    Contract contract = new Contract();
    contract.setProperty(property);
    contract.setTenant(tenant);
    contract.setStartDate(LocalDate.of(2030, 1, 1));
    contract.setEndDate(LocalDate.of(2030, 12, 31));
    contract.setMonthlyRent(new BigDecimal("1000.00"));
    contract.setStatus(status);
    return contracts.save(contract);
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
