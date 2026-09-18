package com.rentmanager.backend.publicapi;

import com.rentmanager.backend.domain.Property;
import com.rentmanager.backend.domain.PropertyStatus;
import com.rentmanager.backend.error.ApiException;
import com.rentmanager.backend.error.ErrorCode;
import com.rentmanager.backend.repository.PropertyRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicPropertyService {

  private final PropertyRepository properties;

  public PublicPropertyService(PropertyRepository properties) {
    this.properties = properties;
  }

  @Transactional(readOnly = true)
  public List<PublicPropertyResponse> search(String query) {
    String term = query == null ? "" : query.trim();
    return properties.searchAvailable(PropertyStatus.AVAILABLE, term).stream()
        .map(PublicPropertyResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public PublicPropertyResponse get(Long id) {
    Property property = properties.findById(id)
        .filter(found -> found.getStatus() == PropertyStatus.AVAILABLE)
        .orElseThrow(() -> new ApiException(ErrorCode.PROPERTY_NOT_FOUND));
    return PublicPropertyResponse.from(property);
  }
}
