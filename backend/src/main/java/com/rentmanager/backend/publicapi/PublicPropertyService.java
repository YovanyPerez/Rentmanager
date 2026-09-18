package com.rentmanager.backend.publicapi;

import com.rentmanager.backend.domain.Property;
import com.rentmanager.backend.domain.PropertyImage;
import com.rentmanager.backend.domain.PropertyStatus;
import com.rentmanager.backend.error.ApiException;
import com.rentmanager.backend.error.ErrorCode;
import com.rentmanager.backend.property.PropertyImageService;
import com.rentmanager.backend.repository.PropertyRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicPropertyService {

  private final PropertyRepository properties;
  private final PropertyImageService imageService;

  public PublicPropertyService(PropertyRepository properties, PropertyImageService imageService) {
    this.properties = properties;
    this.imageService = imageService;
  }

  @Transactional(readOnly = true)
  public List<PublicPropertyResponse> search(String query) {
    String term = query == null ? "" : query.trim();
    List<Property> found = properties.searchAvailable(PropertyStatus.AVAILABLE, term);
    Map<Long, List<PropertyImage>> images =
        imageService.byPropertyIds(found.stream().map(Property::getId).toList());
    return found.stream()
        .map(property -> PublicPropertyResponse.from(property, images.getOrDefault(property.getId(), List.of())))
        .toList();
  }

  @Transactional(readOnly = true)
  public PublicPropertyResponse get(Long id) {
    Property property = properties.findById(id)
        .filter(found -> found.getStatus() == PropertyStatus.AVAILABLE)
        .orElseThrow(() -> new ApiException(ErrorCode.PROPERTY_NOT_FOUND));
    return PublicPropertyResponse.from(property, imageService.byPropertyId(property.getId()));
  }
}
