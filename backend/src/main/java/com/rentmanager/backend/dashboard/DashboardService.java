package com.rentmanager.backend.dashboard;

import com.rentmanager.backend.domain.ContractStatus;
import com.rentmanager.backend.domain.MaintenanceStatus;
import com.rentmanager.backend.domain.PaymentStatus;
import com.rentmanager.backend.domain.PropertyStatus;
import com.rentmanager.backend.repository.ContractRepository;
import com.rentmanager.backend.repository.MaintenanceRequestRepository;
import com.rentmanager.backend.repository.PaymentRepository;
import com.rentmanager.backend.repository.PropertyRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

  private final PropertyRepository properties;
  private final ContractRepository contracts;
  private final PaymentRepository payments;
  private final MaintenanceRequestRepository maintenance;

  public DashboardService(PropertyRepository properties, ContractRepository contracts,
      PaymentRepository payments, MaintenanceRequestRepository maintenance) {
    this.properties = properties;
    this.contracts = contracts;
    this.payments = payments;
    this.maintenance = maintenance;
  }

  /**
   * Statistics defined in AGENTS.md section 14. Monthly income is the amount collected
   * this month (payments marked PAID with a paid date inside the current month).
   */
  @Transactional(readOnly = true)
  public DashboardResponse statistics() {
    LocalDate today = LocalDate.now();
    BigDecimal collected = payments.sumPaidBetween(
        PaymentStatus.PAID, today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()));
    return new DashboardResponse(
        properties.count(),
        properties.countByStatus(PropertyStatus.AVAILABLE),
        properties.countByStatus(PropertyStatus.RENTED),
        contracts.countByStatusAndEndDateGreaterThanEqual(ContractStatus.ACTIVE, today),
        payments.countByStatusAndDueDateGreaterThanEqual(PaymentStatus.PENDING, today),
        payments.countByStatus(PaymentStatus.OVERDUE)
            + payments.countByStatusAndDueDateBefore(PaymentStatus.PENDING, today),
        maintenance.countByStatus(MaintenanceStatus.OPEN),
        collected == null ? BigDecimal.ZERO : collected);
  }
}
