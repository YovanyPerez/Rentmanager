package com.rentmanager.backend.maintenance;

import com.rentmanager.backend.domain.MaintenanceRequest;
import java.time.Instant;

public record MaintenanceResponse(
    Long id,
    Long propertyId,
    String propertyAddress,
    Long createdById,
    String createdByName,
    String title,
    String description,
    String status,
    String assignedTo,
    Instant createdAt,
    Instant updatedAt) {

  static MaintenanceResponse from(MaintenanceRequest request) {
    return new MaintenanceResponse(
        request.getId(),
        request.getProperty().getId(),
        request.getProperty().getAddress(),
        request.getCreatedBy().getId(),
        request.getCreatedBy().getFullName(),
        request.getTitle(),
        request.getDescription(),
        request.getStatus().name(),
        request.getAssignedTo(),
        request.getCreatedAt(),
        request.getUpdatedAt());
  }
}
