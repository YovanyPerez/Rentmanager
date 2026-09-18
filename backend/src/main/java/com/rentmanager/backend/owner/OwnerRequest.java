package com.rentmanager.backend.owner;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record OwnerRequest(
    @NotBlank @Size(max = 150) String fullName,
    @Email @Size(max = 255) String email,
    @Size(max = 50) String phone,
    @Positive Long userId) {}
