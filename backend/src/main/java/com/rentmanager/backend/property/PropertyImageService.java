package com.rentmanager.backend.property;

import com.rentmanager.backend.domain.Property;
import com.rentmanager.backend.domain.PropertyImage;
import com.rentmanager.backend.domain.Role;
import com.rentmanager.backend.domain.User;
import com.rentmanager.backend.error.ApiException;
import com.rentmanager.backend.error.ErrorCode;
import com.rentmanager.backend.repository.PropertyImageRepository;
import com.rentmanager.backend.repository.PropertyRepository;
import com.rentmanager.backend.security.CurrentUser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PropertyImageService {

  public static final int MAX_IMAGES = 6;

  private final PropertyRepository properties;
  private final PropertyImageRepository images;
  private final PropertyImageStorage storage;
  private final CurrentUser currentUser;

  public PropertyImageService(PropertyRepository properties, PropertyImageRepository images,
      PropertyImageStorage storage, CurrentUser currentUser) {
    this.properties = properties;
    this.images = images;
    this.storage = storage;
    this.currentUser = currentUser;
  }

  @Transactional
  public PropertyImageResponse upload(Long propertyId, MultipartFile file) {
    Property property = find(propertyId);
    requireManager(property);
    long count = images.countByPropertyId(propertyId);
    if (count >= MAX_IMAGES) {
      throw new ApiException(ErrorCode.IMAGE_LIMIT_REACHED);
    }
    PropertyImageStorage.StoredImage stored = storage.store(file);
    PropertyImage image = new PropertyImage();
    image.setProperty(property);
    image.setFileName(stored.fileName());
    image.setContentType(stored.contentType());
    image.setPosition((int) count);
    images.save(image);
    return PropertyImageResponse.from(image);
  }

  @Transactional
  public void delete(Long propertyId, Long imageId) {
    Property property = find(propertyId);
    requireManager(property);
    PropertyImage image = findImage(propertyId, imageId);
    images.delete(image);
    storage.delete(image.getFileName());
    applyOrder(images.findByPropertyIdOrderByPositionAscIdAsc(propertyId));
  }

  @Transactional
  public List<PropertyImageResponse> setCover(Long propertyId, Long imageId) {
    Property property = find(propertyId);
    requireManager(property);
    List<PropertyImage> ordered = new ArrayList<>(images.findByPropertyIdOrderByPositionAscIdAsc(propertyId));
    PropertyImage cover = ordered.stream()
        .filter(image -> image.getId().equals(imageId))
        .findFirst()
        .orElseThrow(() -> new ApiException(ErrorCode.IMAGE_NOT_FOUND));
    ordered.remove(cover);
    ordered.add(0, cover);
    return applyOrder(ordered);
  }

  @Transactional
  public List<PropertyImageResponse> reorder(Long propertyId, List<Long> imageIds) {
    Property property = find(propertyId);
    requireManager(property);
    List<PropertyImage> current = images.findByPropertyIdOrderByPositionAscIdAsc(propertyId);
    Set<Long> currentIds = current.stream().map(PropertyImage::getId).collect(Collectors.toSet());
    if (imageIds.size() != current.size() || !currentIds.equals(Set.copyOf(imageIds))) {
      throw new ApiException(ErrorCode.BAD_REQUEST);
    }
    Map<Long, PropertyImage> byId = current.stream()
        .collect(Collectors.toMap(PropertyImage::getId, image -> image));
    return applyOrder(imageIds.stream().map(byId::get).toList());
  }

  @Transactional(readOnly = true)
  public List<PropertyImage> byPropertyId(Long propertyId) {
    return images.findByPropertyIdOrderByPositionAscIdAsc(propertyId);
  }

  @Transactional(readOnly = true)
  public Map<Long, List<PropertyImage>> byPropertyIds(List<Long> propertyIds) {
    if (propertyIds.isEmpty()) {
      return Map.of();
    }
    return images.findByPropertyIdInOrderByPositionAscIdAsc(propertyIds).stream()
        .collect(Collectors.groupingBy(image -> image.getProperty().getId()));
  }

  private List<PropertyImageResponse> applyOrder(List<PropertyImage> ordered) {
    for (int index = 0; index < ordered.size(); index++) {
      ordered.get(index).setPosition(index);
    }
    return ordered.stream()
        .sorted(Comparator.comparingInt(PropertyImage::getPosition))
        .map(PropertyImageResponse::from)
        .toList();
  }

  private Property find(Long propertyId) {
    return properties.findById(propertyId).orElseThrow(() -> new ApiException(ErrorCode.PROPERTY_NOT_FOUND));
  }

  private PropertyImage findImage(Long propertyId, Long imageId) {
    return images.findById(imageId)
        .filter(image -> image.getProperty().getId().equals(propertyId))
        .orElseThrow(() -> new ApiException(ErrorCode.IMAGE_NOT_FOUND));
  }

  /** ADMIN or the OWNER linked to the property. */
  private void requireManager(Property property) {
    User user = currentUser.get();
    if (user.getRole() == Role.ADMIN) {
      return;
    }
    boolean owns = property.getOwner().getUser() != null
        && user.getId().equals(property.getOwner().getUser().getId());
    if (user.getRole() != Role.OWNER || !owns) {
      throw new ApiException(ErrorCode.FORBIDDEN);
    }
  }
}
