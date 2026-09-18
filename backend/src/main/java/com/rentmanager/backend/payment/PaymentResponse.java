package com.rentmanager.backend.payment;

import com.rentmanager.backend.domain.Payment;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PaymentResponse(
    Long id,
    Long contractId,
    Long propertyId,
    String propertyAddress,
    String tenantName,
    BigDecimal amount,
    String currency,
    LocalDate dueDate,
    LocalDate paidDate,
    String status,
    Instant createdAt,
    Instant updatedAt) {

  static PaymentResponse from(Payment payment) {
    return new PaymentResponse(
        payment.getId(),
        payment.getContract().getId(),
        payment.getContract().getProperty().getId(),
        payment.getContract().getProperty().getAddress(),
        payment.getContract().getTenant().getFullName(),
        payment.getAmount(),
        payment.getContract().getCurrency().name(),
        payment.getDueDate(),
        payment.getPaidDate(),
        payment.getStatus().name(),
        payment.getCreatedAt(),
        payment.getUpdatedAt());
  }
}
