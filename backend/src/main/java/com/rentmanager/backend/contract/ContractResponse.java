package com.rentmanager.backend.contract;

import com.rentmanager.backend.domain.Contract;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ContractResponse(
    Long id,
    Long propertyId,
    String propertyAddress,
    Long tenantId,
    String tenantName,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal monthlyRent,
    String status,
    Instant createdAt,
    Instant updatedAt) {

  static ContractResponse from(Contract contract) {
    return new ContractResponse(
        contract.getId(),
        contract.getProperty().getId(),
        contract.getProperty().getAddress(),
        contract.getTenant().getId(),
        contract.getTenant().getFullName(),
        contract.getStartDate(),
        contract.getEndDate(),
        contract.getMonthlyRent(),
        contract.getStatus().name(),
        contract.getCreatedAt(),
        contract.getUpdatedAt());
  }
}
