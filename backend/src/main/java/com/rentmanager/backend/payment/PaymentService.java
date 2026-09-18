package com.rentmanager.backend.payment;

import com.rentmanager.backend.domain.Contract;
import com.rentmanager.backend.domain.Payment;
import com.rentmanager.backend.domain.PaymentStatus;
import com.rentmanager.backend.domain.Role;
import com.rentmanager.backend.domain.User;
import com.rentmanager.backend.error.ApiException;
import com.rentmanager.backend.error.ErrorCode;
import com.rentmanager.backend.repository.ContractRepository;
import com.rentmanager.backend.repository.PaymentRepository;
import com.rentmanager.backend.security.CurrentUser;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

  /**
   * Manual transitions (AGENTS.md section 9). PENDING -> OVERDUE is set by the system
   * (evaluated at read time) and PAID/CANCELLED are final.
   */
  private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS = Map.of(
      PaymentStatus.PENDING, Set.of(PaymentStatus.PAID, PaymentStatus.CANCELLED),
      PaymentStatus.OVERDUE, Set.of(PaymentStatus.PAID, PaymentStatus.CANCELLED),
      PaymentStatus.PAID, Set.of(),
      PaymentStatus.CANCELLED, Set.of());

  private final PaymentRepository payments;
  private final ContractRepository contracts;
  private final CurrentUser currentUser;

  public PaymentService(PaymentRepository payments, ContractRepository contracts, CurrentUser currentUser) {
    this.payments = payments;
    this.contracts = contracts;
    this.currentUser = currentUser;
  }

  @Transactional
  public List<PaymentResponse> list() {
    markOverdue();
    User user = currentUser.get();
    List<Payment> found = switch (user.getRole()) {
      case ADMIN -> payments.findAll();
      case OWNER -> payments.findAllByContractPropertyOwnerUserId(user.getId());
      case TENANT -> payments.findAllByContractTenantUserId(user.getId());
    };
    return found.stream().map(PaymentResponse::from).toList();
  }

  @Transactional
  public PaymentResponse get(Long id) {
    markOverdue();
    Payment payment = find(id);
    if (!canRead(payment, currentUser.get())) {
      throw new ApiException(ErrorCode.FORBIDDEN);
    }
    return PaymentResponse.from(payment);
  }

  @Transactional
  public PaymentResponse create(PaymentRequest request) {
    Contract contract = contracts.findById(request.contractId())
        .orElseThrow(() -> new ApiException(ErrorCode.CONTRACT_NOT_FOUND));
    if (payments.existsByContractIdAndDueDate(contract.getId(), request.dueDate())) {
      throw new ApiException(ErrorCode.PAYMENT_ALREADY_EXISTS);
    }
    Payment payment = new Payment();
    payment.setContract(contract);
    payment.setAmount(request.amount());
    payment.setDueDate(request.dueDate());
    payment.setStatus(PaymentStatus.PENDING);
    payments.save(payment);
    return PaymentResponse.from(payment);
  }

  @Transactional
  public PaymentResponse update(Long id, PaymentUpdateRequest request) {
    Payment payment = find(id);
    PaymentStatus current = payment.getStatus();
    PaymentStatus target = request.status();
    if (current == target) {
      return PaymentResponse.from(payment);
    }
    if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
      throw new ApiException(ErrorCode.INVALID_STATE_TRANSITION);
    }
    payment.setStatus(target);
    if (target == PaymentStatus.PAID) {
      payment.setPaidDate(request.paidDate() == null ? LocalDate.now() : request.paidDate());
    }
    return PaymentResponse.from(payment);
  }

  /** Overdue is evaluated at read time (AGENTS.md section 9). */
  private void markOverdue() {
    List<Payment> overdue = payments.findAllByStatusAndDueDateBefore(PaymentStatus.PENDING, LocalDate.now());
    for (Payment payment : overdue) {
      payment.setStatus(PaymentStatus.OVERDUE);
    }
  }

  private Payment find(Long id) {
    return payments.findById(id).orElseThrow(() -> new ApiException(ErrorCode.PAYMENT_NOT_FOUND));
  }

  private static boolean canRead(Payment payment, User user) {
    Contract contract = payment.getContract();
    return switch (user.getRole()) {
      case ADMIN -> true;
      case OWNER -> contract.getProperty().getOwner().getUser() != null
          && user.getId().equals(contract.getProperty().getOwner().getUser().getId());
      case TENANT -> contract.getTenant().getUser() != null
          && user.getId().equals(contract.getTenant().getUser().getId());
    };
  }
}
