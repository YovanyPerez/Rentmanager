package com.rentmanager.backend.property;

import com.rentmanager.backend.domain.PropertyImage;

/** Public URL of a stored property image. */
public record PropertyImageResponse(Long id, String url, int position) {

  public static final String URL_PREFIX = "/uploads/properties/";

  public static PropertyImageResponse from(PropertyImage image) {
    return new PropertyImageResponse(image.getId(), URL_PREFIX + image.getFileName(), image.getPosition());
  }
}
