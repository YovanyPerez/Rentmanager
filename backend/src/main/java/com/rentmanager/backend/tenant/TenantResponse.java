package com.rentmanager.backend.tenant;

import com.rentmanager.backend.domain.Tenant;
import java.time.Instant;

public record TenantResponse(
    Long id,
    String fullName,
    String email,
    String phone,
    Long userId,
    Instant createdAt) {

  static TenantResponse from(Tenant tenant) {
    return new TenantResponse(
        tenant.getId(),
        tenant.getFullName(),
        tenant.getEmail(),
        tenant.getPhone(),
        tenant.getUser() == null ? null : tenant.getUser().getId(),
        tenant.getCreatedAt());
  }
}
