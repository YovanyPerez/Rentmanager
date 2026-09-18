package com.rentmanager.backend.property;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PropertyImageOrderRequest(
    @NotNull @Size(min = 1, max = PropertyImageService.MAX_IMAGES) List<Long> imageIds) {}
