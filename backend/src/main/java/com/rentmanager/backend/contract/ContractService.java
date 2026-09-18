package com.rentmanager.backend.contract;

import com.rentmanager.backend.domain.Contract;
import com.rentmanager.backend.domain.ContractStatus;
import com.rentmanager.backend.domain.Payment;
import com.rentmanager.backend.domain.PaymentStatus;
import com.rentmanager.backend.domain.Property;
import com.rentmanager.backend.domain.PropertyStatus;
import com.rentmanager.backend.domain.Role;
import com.rentmanager.backend.domain.User;
import com.rentmanager.backend.error.ApiException;
import com.rentmanager.backend.error.ErrorCode;
import com.rentmanager.backend.repository.ContractRepository;
import com.rentmanager.backend.repository.PaymentRepository;
import com.rentmanager.backend.repository.PropertyRepository;
import com.rentmanager.backend.repository.TenantRepository;
import com.rentmanager.backend.security.CurrentUser;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractService {

  private final ContractRepository contracts;
  private final PropertyRepository properties;
  private final TenantRepository tenants;
  private final PaymentRepository payments;
  private final CurrentUser currentUser;

  public ContractService(ContractRepository contracts, PropertyRepository properties,
      TenantRepository tenants, PaymentRepository payments, CurrentUser currentUser) {
    this.contracts = contracts;
    this.properties = properties;
    this.tenants = tenants;
    this.payments = payments;
    this.currentUser = currentUser;
  }

  @Transactional
  public List<ContractResponse> list() {
    expireOverdueContracts();
    User user = currentUser.get();
    List<Contract> found = switch (user.getRole()) {
      case ADMIN -> contracts.findAll();
      case OWNER -> contracts.findAllByPropertyOwnerUserId(user.getId());
      case TENANT -> contracts.findAllByTenantUserId(user.getId());
    };
    return found.stream().map(ContractResponse::from).toList();
  }

  @Transactional
  public ContractResponse get(Long id) {
    expireOverdueContracts();
    Contract contract = find(id);
    if (!canRead(contract, currentUser.get())) {
      throw new ApiException(ErrorCode.FORBIDDEN);
    }
    return ContractResponse.from(contract);
  }

  @Transactional
  public ContractResponse create(ContractRequest request) {
    Contract contract = new Contract();
    applyRequest(contract, request);
    contract.setStatus(ContractStatus.DRAFT);
    contracts.save(contract);
    return ContractResponse.from(contract);
  }

  @Transactional
  public ContractResponse update(Long id, ContractRequest request) {
    Contract contract = find(id);
    requireStatus(contract, ContractStatus.DRAFT);
    applyRequest(contract, request);
    return ContractResponse.from(contract);
  }

  @Transactional
  public ContractResponse activate(Long id) {
    Contract contract = find(id);
    requireStatus(contract, ContractStatus.DRAFT);
    Property property = contract.getProperty();
    if (contracts.existsByPropertyIdAndStatus(property.getId(), ContractStatus.ACTIVE)) {
      throw new ApiException(ErrorCode.CONFLICT_ACTIVE_CONTRACT);
    }
    if (property.getStatus() != PropertyStatus.AVAILABLE) {
      throw new ApiException(ErrorCode.INVALID_STATE_TRANSITION);
    }
    contract.setStatus(ContractStatus.ACTIVE);
    property.setStatus(PropertyStatus.RENTED);
    generateInstallments(contract);
    return ContractResponse.from(contract);
  }

  @Transactional
  public ContractResponse terminate(Long id) {
    Contract contract = find(id);
    requireStatus(contract, ContractStatus.ACTIVE);
    contract.setStatus(ContractStatus.TERMINATED);
    releaseProperty(contract);
    return ContractResponse.from(contract);
  }

  @Transactional
  public void delete(Long id) {
    Contract contract = find(id);
    requireStatus(contract, ContractStatus.DRAFT);
    contracts.delete(contract);
  }

  /** Expiration is evaluated at read time (AGENTS.md section 8). */
  private void expireOverdueContracts() {
    List<Contract> overdue = contracts.findAllByStatusAndEndDateBefore(ContractStatus.ACTIVE, LocalDate.now());
    for (Contract contract : overdue) {
      contract.setStatus(ContractStatus.EXPIRED);
      releaseProperty(contract);
    }
  }

  /** One installment per month, due the same day of each month (AGENTS.md section 9). */
  private void generateInstallments(Contract contract) {
    LocalDate dueDate = contract.getStartDate();
    while (!dueDate.isAfter(contract.getEndDate())) {
      Payment payment = new Payment();
      payment.setContract(contract);
      payment.setAmount(contract.getMonthlyRent());
      payment.setDueDate(dueDate);
      payment.setStatus(PaymentStatus.PENDING);
      payments.save(payment);
      dueDate = dueDate.plusMonths(1);
    }
  }

  private void applyRequest(Contract contract, ContractRequest request) {
    if (!request.startDate().isBefore(request.endDate())) {
      throw new ApiException(ErrorCode.INVALID_DATE_RANGE);
    }
    contract.setProperty(properties.findById(request.propertyId())
        .orElseThrow(() -> new ApiException(ErrorCode.PROPERTY_NOT_FOUND)));
    contract.setTenant(tenants.findById(request.tenantId())
        .orElseThrow(() -> new ApiException(ErrorCode.TENANT_NOT_FOUND)));
    contract.setStartDate(request.startDate());
    contract.setEndDate(request.endDate());
    contract.setMonthlyRent(request.monthlyRent());
    contract.setCurrency(contract.getProperty().getCurrency());
  }

  private Contract find(Long id) {
    return contracts.findById(id).orElseThrow(() -> new ApiException(ErrorCode.CONTRACT_NOT_FOUND));
  }

  private static void requireStatus(Contract contract, ContractStatus expected) {
    if (contract.getStatus() != expected) {
      throw new ApiException(ErrorCode.INVALID_STATE_TRANSITION);
    }
  }

  private static void releaseProperty(Contract contract) {
    Property property = contract.getProperty();
    if (property.getStatus() == PropertyStatus.RENTED) {
      property.setStatus(PropertyStatus.AVAILABLE);
    }
  }

  private static boolean canRead(Contract contract, User user) {
    return switch (user.getRole()) {
      case ADMIN -> true;
      case OWNER -> contract.getProperty().getOwner().getUser() != null
          && user.getId().equals(contract.getProperty().getOwner().getUser().getId());
      case TENANT -> contract.getTenant().getUser() != null
          && user.getId().equals(contract.getTenant().getUser().getId());
    };
  }
}
