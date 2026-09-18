package com.rentmanager.backend.contract;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractRequest(
    @NotNull @Positive Long propertyId,
    @NotNull @Positive Long tenantId,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal monthlyRent) {}
