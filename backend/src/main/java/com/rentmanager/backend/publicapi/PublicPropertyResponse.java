package com.rentmanager.backend.publicapi;

import com.rentmanager.backend.domain.Property;
import java.math.BigDecimal;

/** Anonymous view of a property: no owner data, no internal ids beyond the property itself. */
public record PublicPropertyResponse(
    Long id,
    String address,
    String city,
    String description,
    BigDecimal monthlyRent,
    String status) {

  static PublicPropertyResponse from(Property property) {
    return new PublicPropertyResponse(
        property.getId(),
        property.getAddress(),
        property.getCity(),
        property.getDescription(),
        property.getMonthlyRent(),
        property.getStatus().name());
  }
}
