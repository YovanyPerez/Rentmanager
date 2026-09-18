package com.rentmanager.backend.property;

import com.rentmanager.backend.domain.Property;
import com.rentmanager.backend.domain.PropertyImage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PropertyResponse(
    Long id,
    Long ownerId,
    String ownerName,
    String address,
    String city,
    String description,
    String imageUrl,
    List<PropertyImageResponse> images,
    BigDecimal monthlyRent,
    String currency,
    String status,
    Instant createdAt,
    Instant updatedAt) {

  static PropertyResponse from(Property property, List<PropertyImage> images) {
    List<PropertyImageResponse> mapped = images.stream().map(PropertyImageResponse::from).toList();
    return new PropertyResponse(
        property.getId(),
        property.getOwner().getId(),
        property.getOwner().getFullName(),
        property.getAddress(),
        property.getCity(),
        property.getDescription(),
        mapped.isEmpty() ? null : mapped.get(0).url(),
        mapped,
        property.getMonthlyRent(),
        property.getCurrency().name(),
        property.getStatus().name(),
        property.getCreatedAt(),
        property.getUpdatedAt());
  }
}
