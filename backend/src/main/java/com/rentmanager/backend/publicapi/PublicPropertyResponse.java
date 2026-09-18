package com.rentmanager.backend.publicapi;

import com.rentmanager.backend.domain.Property;
import com.rentmanager.backend.domain.PropertyImage;
import com.rentmanager.backend.property.PropertyImageResponse;
import java.math.BigDecimal;
import java.util.List;

/** Anonymous view of a property: no owner data, no internal ids beyond the property itself. */
public record PublicPropertyResponse(
    Long id,
    String address,
    String city,
    String description,
    String imageUrl,
    List<PropertyImageResponse> images,
    BigDecimal monthlyRent,
    String currency,
    String status) {

  static PublicPropertyResponse from(Property property, List<PropertyImage> images) {
    List<PropertyImageResponse> mapped = images.stream().map(PropertyImageResponse::from).toList();
    return new PublicPropertyResponse(
        property.getId(),
        property.getAddress(),
        property.getCity(),
        property.getDescription(),
        mapped.isEmpty() ? null : mapped.get(0).url(),
        mapped,
        property.getMonthlyRent(),
        property.getCurrency().name(),
        property.getStatus().name());
  }
}
