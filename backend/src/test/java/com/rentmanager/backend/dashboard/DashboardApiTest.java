package com.rentmanager.backend.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.rentmanager.backend.domain.Contract;
import com.rentmanager.backend.domain.ContractStatus;
import com.rentmanager.backend.domain.CurrencyCode;
import com.rentmanager.backend.domain.MaintenanceRequest;
import com.rentmanager.backend.domain.MaintenanceStatus;
import com.rentmanager.backend.domain.Owner;
import com.rentmanager.backend.domain.Payment;
import com.rentmanager.backend.domain.PaymentStatus;
import com.rentmanager.backend.domain.Property;
import com.rentmanager.backend.domain.PropertyStatus;
import com.rentmanager.backend.domain.Role;
import com.rentmanager.backend.domain.Tenant;
import com.rentmanager.backend.domain.User;
import com.rentmanager.backend.repository.ContractRepository;
import com.rentmanager.backend.repository.MaintenanceRequestRepository;
import com.rentmanager.backend.repository.OwnerRepository;
import com.rentmanager.backend.repository.PaymentRepository;
import com.rentmanager.backend.repository.PropertyRepository;
import com.rentmanager.backend.repository.TenantRepository;
import com.rentmanager.backend.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
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
class DashboardApiTest {

  private static final String PASSWORD = "secret123";
  private static final String ADMIN = "admin-dashboard@test.local";
  private static final String OWNER_USER = "owner-dashboard@test.local";
  private static final String TENANT_USER = "tenant-dashboard@test.local";

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
  private MaintenanceRequestRepository maintenance;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Test
  void dashboardIsAdminOnly() throws Exception {
    String adminToken = login(ADMIN, Role.ADMIN);
    mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());

    String ownerToken = login(OWNER_USER, Role.OWNER);
    mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + ownerToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));

    String tenantToken = login(TENANT_USER, Role.TENANT);
    mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + tenantToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void statisticsReflectDataChanges() throws Exception {
    String token = login(ADMIN, Role.ADMIN);
    Dashboard before = readDashboard(token);

    Owner owner = newOwner(OWNER_USER);
    newProperty(owner, PropertyStatus.AVAILABLE);
    Property rented = newProperty(owner, PropertyStatus.RENTED);
    Tenant tenant = newTenant(TENANT_USER);
    Contract contract = newContract(rented, tenant);
    newPayment(contract, "500.00", LocalDate.now(), PaymentStatus.PAID, LocalDate.now());
    newPayment(contract, "500.00", LocalDate.now().plusDays(5), PaymentStatus.PENDING, null);
    newPayment(contract, "500.00", LocalDate.now().minusDays(5), PaymentStatus.PENDING, null);
    User adminUser = users.findByEmail(ADMIN).orElseThrow();
    newMaintenance(rented, adminUser, MaintenanceStatus.OPEN);
    newMaintenance(rented, adminUser, MaintenanceStatus.COMPLETED);

    Dashboard after = readDashboard(token);

    assertThat(after.totalProperties() - before.totalProperties()).isEqualTo(2);
    assertThat(after.availableProperties() - before.availableProperties()).isEqualTo(1);
    assertThat(after.rentedProperties() - before.rentedProperties()).isEqualTo(1);
    assertThat(after.activeContracts() - before.activeContracts()).isEqualTo(1);
    assertThat(after.pendingPayments() - before.pendingPayments()).isEqualTo(1);
    assertThat(after.overduePayments() - before.overduePayments()).isEqualTo(1);
    assertThat(after.openMaintenanceRequests() - before.openMaintenanceRequests()).isEqualTo(1);
    assertThat(after.income("COP") - before.income("COP")).isEqualTo(500.0);
  }

  @Test
  void monthlyIncomeExcludesOtherMonths() throws Exception {
    String token = login(ADMIN, Role.ADMIN);
    Dashboard before = readDashboard(token);

    Tenant tenant = newTenant(TENANT_USER);
    Contract contract = newContract(newProperty(newOwner(OWNER_USER), PropertyStatus.AVAILABLE), tenant);
    LocalDate lastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(10);
    newPayment(contract, "700.00", lastMonth, PaymentStatus.PAID, lastMonth);

    Dashboard after = readDashboard(token);

    assertThat(after.income("COP") - before.income("COP")).isEqualTo(0.0);
  }

  @Test
  void monthlyIncomeIsGroupedByCurrency() throws Exception {
    String token = login(ADMIN, Role.ADMIN);
    Dashboard before = readDashboard(token);

    Property euroProperty = newProperty(newOwner(OWNER_USER), PropertyStatus.AVAILABLE);
    euroProperty.setCurrency(CurrencyCode.EUR);
    properties.save(euroProperty);
    Contract contract = newContract(euroProperty, newTenant(TENANT_USER));
    newPayment(contract, "300.00", LocalDate.now(), PaymentStatus.PAID, LocalDate.now());

    Dashboard after = readDashboard(token);

    assertThat(after.income("EUR") - before.income("EUR")).isEqualTo(300.0);
    assertThat(after.income("COP") - before.income("COP")).isEqualTo(0.0);
  }

  private Dashboard readDashboard(String token) throws Exception {
    String response = mockMvc.perform(get("/api/dashboard")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    Map<String, Double> income = new HashMap<>();
    List<Map<String, Object>> rows = JsonPath.read(response, "$.monthlyIncome");
    for (Map<String, Object> row : rows) {
      income.put((String) row.get("currency"), ((Number) row.get("total")).doubleValue());
    }
    return new Dashboard(
        ((Number) JsonPath.read(response, "$.totalProperties")).longValue(),
        ((Number) JsonPath.read(response, "$.availableProperties")).longValue(),
        ((Number) JsonPath.read(response, "$.rentedProperties")).longValue(),
        ((Number) JsonPath.read(response, "$.activeContracts")).longValue(),
        ((Number) JsonPath.read(response, "$.pendingPayments")).longValue(),
        ((Number) JsonPath.read(response, "$.overduePayments")).longValue(),
        ((Number) JsonPath.read(response, "$.openMaintenanceRequests")).longValue(),
        income);
  }

  private record Dashboard(long totalProperties, long availableProperties, long rentedProperties,
      long activeContracts, long pendingPayments, long overduePayments,
      long openMaintenanceRequests, Map<String, Double> monthlyIncome) {

    double income(String currency) {
      return monthlyIncome.getOrDefault(currency, 0.0);
    }
  }

  private void newMaintenance(Property property, User createdBy, MaintenanceStatus status) {
    MaintenanceRequest request = new MaintenanceRequest();
    request.setProperty(property);
    request.setCreatedBy(createdBy);
    request.setTitle("Dashboard test");
    request.setStatus(status);
    maintenance.save(request);
  }

  private void newPayment(Contract contract, String amount, LocalDate dueDate,
      PaymentStatus status, LocalDate paidDate) {
    Payment payment = new Payment();
    payment.setContract(contract);
    payment.setAmount(new BigDecimal(amount));
    payment.setDueDate(dueDate);
    payment.setStatus(status);
    payment.setPaidDate(paidDate);
    payments.save(payment);
  }

  private Contract newContract(Property property, Tenant tenant) {
    Contract contract = new Contract();
    contract.setProperty(property);
    contract.setTenant(tenant);
    contract.setStartDate(LocalDate.of(2030, 1, 1));
    contract.setEndDate(LocalDate.of(2030, 12, 31));
    contract.setMonthlyRent(new BigDecimal("1000.00"));
    contract.setCurrency(property.getCurrency());
    contract.setStatus(ContractStatus.ACTIVE);
    return contracts.save(contract);
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

  private Property newProperty(Owner owner, PropertyStatus status) {
    Property property = new Property();
    property.setOwner(owner);
    property.setAddress("Dashboard test " + System.nanoTime());
    property.setCity("Madrid");
    property.setMonthlyRent(new BigDecimal("1000.00"));
    property.setStatus(status);
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
