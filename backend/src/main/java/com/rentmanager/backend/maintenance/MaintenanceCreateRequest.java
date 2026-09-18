package com.rentmanager.backend.maintenance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MaintenanceCreateRequest(
    @NotNull @Positive Long propertyId,
    @NotBlank @Size(max = 150) String title,
    @Size(max = 2000) String description) {}
