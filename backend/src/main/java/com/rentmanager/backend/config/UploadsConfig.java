package com.rentmanager.backend.config;

import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serves the uploaded property images; the files are public content. */
@Configuration
public class UploadsConfig implements WebMvcConfigurer {

  private final String location;

  public UploadsConfig(@Value("${app.uploads.dir:./uploads}") String uploadsDir) {
    String uri = Paths.get(uploadsDir).toAbsolutePath().normalize().toUri().toString();
    this.location = uri.endsWith("/") ? uri : uri + "/";
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/uploads/**").addResourceLocations(location);
  }
}
