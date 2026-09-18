package com.rentmanager.backend.property;

import com.rentmanager.backend.domain.Owner;
import com.rentmanager.backend.domain.Property;
import com.rentmanager.backend.domain.PropertyImage;
import com.rentmanager.backend.domain.PropertyStatus;
import com.rentmanager.backend.domain.Role;
import com.rentmanager.backend.domain.User;
import com.rentmanager.backend.error.ApiException;
import com.rentmanager.backend.error.ErrorCode;
import com.rentmanager.backend.repository.OwnerRepository;
import com.rentmanager.backend.repository.PropertyRepository;
import com.rentmanager.backend.security.CurrentUser;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropertyService {

  /**
   * Manual status transitions (AGENTS.md section 7). AVAILABLE -> RENTED is missing on purpose:
   * only contract activation may mark a property as rented.
   */
  private static final Map<PropertyStatus, Set<PropertyStatus>> ALLOWED_TRANSITIONS = Map.of(
      PropertyStatus.AVAILABLE, Set.of(PropertyStatus.MAINTENANCE, PropertyStatus.INACTIVE),
      PropertyStatus.MAINTENANCE, Set.of(PropertyStatus.AVAILABLE),
      PropertyStatus.INACTIVE, Set.of(PropertyStatus.AVAILABLE),
      PropertyStatus.RENTED, Set.of(PropertyStatus.AVAILABLE));

  private final PropertyRepository properties;
  private final OwnerRepository owners;
  private final PropertyImageService imageService;
  private final CurrentUser currentUser;

  public PropertyService(PropertyRepository properties, OwnerRepository owners,
      PropertyImageService imageService, CurrentUser currentUser) {
    this.properties = properties;
    this.owners = owners;
    this.imageService = imageService;
    this.currentUser = currentUser;
  }

  @Transactional(readOnly = true)
  public List<PropertyResponse> list() {
    User user = currentUser.get();
    List<Property> found = user.getRole() == Role.ADMIN
        ? properties.findAll()
        : properties.findAllByOwnerUserId(user.getId());
    Map<Long, List<PropertyImage>> images =
        imageService.byPropertyIds(found.stream().map(Property::getId).toList());
    return found.stream()
        .map(property -> PropertyResponse.from(property, images.getOrDefault(property.getId(), List.of())))
        .toList();
  }

  @Transactional(readOnly = true)
  public PropertyResponse get(Long id) {
    Property property = find(id);
    User user = currentUser.get();
    if (user.getRole() != Role.ADMIN && !isOwnedBy(property, user)) {
      throw new ApiException(ErrorCode.FORBIDDEN);
    }
    return PropertyResponse.from(property, imageService.byPropertyId(property.getId()));
  }

  @Transactional
  public PropertyResponse create(PropertyRequest request) {
    Property property = new Property();
    property.setOwner(findOwner(request.ownerId()));
    applyRequest(property, request);
    property.setStatus(PropertyStatus.AVAILABLE);
    properties.save(property);
    return PropertyResponse.from(property, List.of());
  }

  @Transactional
  public PropertyResponse update(Long id, PropertyRequest request) {
    Property property = find(id);
    if (!property.getOwner().getId().equals(request.ownerId())) {
      property.setOwner(findOwner(request.ownerId()));
    }
    applyRequest(property, request);
    return PropertyResponse.from(property, imageService.byPropertyId(property.getId()));
  }

  @Transactional
  public PropertyResponse changeStatus(Long id, PropertyStatus target) {
    Property property = find(id);
    List<PropertyImage> images = imageService.byPropertyId(property.getId());
    PropertyStatus current = property.getStatus();
    if (current == target) {
      return PropertyResponse.from(property, images);
    }
    if (target == PropertyStatus.RENTED
        || !ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
      throw new ApiException(ErrorCode.INVALID_STATE_TRANSITION);
    }
    property.setStatus(target);
    return PropertyResponse.from(property, images);
  }

  /** DELETE deactivates the property; rented properties cannot be deactivated. */
  @Transactional
  public void deactivate(Long id) {
    Property property = find(id);
    if (property.getStatus() == PropertyStatus.RENTED) {
      throw new ApiException(ErrorCode.INVALID_STATE_TRANSITION);
    }
    property.setStatus(PropertyStatus.INACTIVE);
  }

  private Property find(Long id) {
    return properties.findById(id).orElseThrow(() -> new ApiException(ErrorCode.PROPERTY_NOT_FOUND));
  }

  private Owner findOwner(Long id) {
    return owners.findById(id).orElseThrow(() -> new ApiException(ErrorCode.OWNER_NOT_FOUND));
  }

  private static boolean isOwnedBy(Property property, User user) {
    return property.getOwner().getUser() != null
        && user.getId().equals(property.getOwner().getUser().getId());
  }

  private static void applyRequest(Property property, PropertyRequest request) {
    property.setAddress(request.address());
    property.setCity(request.city());
    property.setDescription(request.description());
    property.setMonthlyRent(request.monthlyRent());
  }
}
