package com.rentmanager.backend.property;

import com.rentmanager.backend.domain.Property;
import java.math.BigDecimal;
import java.time.Instant;

public record PropertyResponse(
    Long id,
    Long ownerId,
    String ownerName,
    String address,
    String city,
    String description,
    String imageUrl,
    BigDecimal monthlyRent,
    String status,
    Instant createdAt,
    Instant updatedAt) {

  static PropertyResponse from(Property property) {
    return new PropertyResponse(
        property.getId(),
        property.getOwner().getId(),
        property.getOwner().getFullName(),
        property.getAddress(),
        property.getCity(),
        property.getDescription(),
        property.getImageUrl(),
        property.getMonthlyRent(),
        property.getStatus().name(),
        property.getCreatedAt(),
        property.getUpdatedAt());
  }
}
