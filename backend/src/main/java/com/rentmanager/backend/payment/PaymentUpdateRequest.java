package com.rentmanager.backend.payment;

import com.rentmanager.backend.domain.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PaymentUpdateRequest(
    @NotNull PaymentStatus status,
    LocalDate paidDate) {}
