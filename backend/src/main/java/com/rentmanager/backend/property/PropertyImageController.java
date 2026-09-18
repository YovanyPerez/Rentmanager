package com.rentmanager.backend.property;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/properties/{propertyId}/images")
public class PropertyImageController {

  private final PropertyImageService imageService;

  public PropertyImageController(PropertyImageService imageService) {
    this.imageService = imageService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PropertyImageResponse upload(@PathVariable Long propertyId,
      @RequestParam("file") MultipartFile file) {
    return imageService.upload(propertyId, file);
  }

  @DeleteMapping("/{imageId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long propertyId, @PathVariable Long imageId) {
    imageService.delete(propertyId, imageId);
  }

  @PutMapping("/{imageId}/cover")
  public List<PropertyImageResponse> setCover(@PathVariable Long propertyId,
      @PathVariable Long imageId) {
    return imageService.setCover(propertyId, imageId);
  }

  @PutMapping("/order")
  public List<PropertyImageResponse> reorder(@PathVariable Long propertyId,
      @Valid @RequestBody PropertyImageOrderRequest request) {
    return imageService.reorder(propertyId, request.imageIds());
  }
}
