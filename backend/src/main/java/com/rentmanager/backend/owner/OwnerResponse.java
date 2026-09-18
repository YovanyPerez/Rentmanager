package com.rentmanager.backend.owner;

import com.rentmanager.backend.domain.Owner;
import java.time.Instant;

public record OwnerResponse(
    Long id,
    String fullName,
    String email,
    String phone,
    Long userId,
    Instant createdAt) {

  static OwnerResponse from(Owner owner) {
    return new OwnerResponse(
        owner.getId(),
        owner.getFullName(),
        owner.getEmail(),
        owner.getPhone(),
        owner.getUser() == null ? null : owner.getUser().getId(),
        owner.getCreatedAt());
  }
}
