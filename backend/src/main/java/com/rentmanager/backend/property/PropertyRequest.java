package com.rentmanager.backend.property;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PropertyRequest(
    @NotNull @Positive Long ownerId,
    @NotBlank @Size(max = 255) String address,
    @NotBlank @Size(max = 100) String city,
    @Size(max = 2000) String description,
    @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal monthlyRent) {}
