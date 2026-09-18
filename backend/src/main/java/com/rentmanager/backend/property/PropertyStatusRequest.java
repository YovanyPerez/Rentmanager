package com.rentmanager.backend.property;

import com.rentmanager.backend.domain.PropertyStatus;
import jakarta.validation.constraints.NotNull;

public record PropertyStatusRequest(@NotNull PropertyStatus status) {}
