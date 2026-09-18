package com.rentmanager.backend.maintenance;

import com.rentmanager.backend.domain.MaintenanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MaintenanceUpdateRequest(
    @NotNull MaintenanceStatus status,
    @Size(max = 150) String assignedTo) {}
