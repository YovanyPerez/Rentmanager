package com.rentmanager.backend.maintenance;

import com.rentmanager.backend.domain.ContractStatus;
import com.rentmanager.backend.domain.MaintenanceRequest;
import com.rentmanager.backend.domain.MaintenanceStatus;
import com.rentmanager.backend.domain.Property;
import com.rentmanager.backend.domain.Role;
import com.rentmanager.backend.domain.Tenant;
import com.rentmanager.backend.domain.User;
import com.rentmanager.backend.error.ApiException;
import com.rentmanager.backend.error.ErrorCode;
import com.rentmanager.backend.repository.ContractRepository;
import com.rentmanager.backend.repository.MaintenanceRequestRepository;
import com.rentmanager.backend.repository.PropertyRepository;
import com.rentmanager.backend.repository.TenantRepository;
import com.rentmanager.backend.security.CurrentUser;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceService {

  /** Allowed transitions (AGENTS.md section 10). COMPLETED and CANCELLED are final. */
  private static final Map<MaintenanceStatus, Set<MaintenanceStatus>> ALLOWED_TRANSITIONS = Map.of(
      MaintenanceStatus.OPEN, Set.of(MaintenanceStatus.IN_PROGRESS, MaintenanceStatus.CANCELLED),
      MaintenanceStatus.IN_PROGRESS, Set.of(MaintenanceStatus.COMPLETED, MaintenanceStatus.CANCELLED),
      MaintenanceStatus.COMPLETED, Set.of(),
      MaintenanceStatus.CANCELLED, Set.of());

  private final MaintenanceRequestRepository requests;
  private final PropertyRepository properties;
  private final TenantRepository tenants;
  private final ContractRepository contracts;
  private final CurrentUser currentUser;

  public MaintenanceService(MaintenanceRequestRepository requests, PropertyRepository properties,
      TenantRepository tenants, ContractRepository contracts, CurrentUser currentUser) {
    this.requests = requests;
    this.properties = properties;
    this.tenants = tenants;
    this.contracts = contracts;
    this.currentUser = currentUser;
  }

  @Transactional(readOnly = true)
  public List<MaintenanceResponse> list() {
    User user = currentUser.get();
    List<MaintenanceRequest> found = switch (user.getRole()) {
      case ADMIN -> requests.findAll();
      case OWNER -> requests.findAllByPropertyOwnerUserId(user.getId());
      case TENANT -> requests.findAllByCreatedById(user.getId());
    };
    return found.stream().map(MaintenanceResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public MaintenanceResponse get(Long id) {
    MaintenanceRequest request = find(id);
    if (!canRead(request, currentUser.get())) {
      throw new ApiException(ErrorCode.FORBIDDEN);
    }
    return MaintenanceResponse.from(request);
  }

  @Transactional
  public MaintenanceResponse create(MaintenanceCreateRequest createRequest) {
    User user = currentUser.get();
    Property property = properties.findById(createRequest.propertyId())
        .orElseThrow(() -> new ApiException(ErrorCode.PROPERTY_NOT_FOUND));
    if (user.getRole() == Role.TENANT && !isCurrentlyRenting(user, property)) {
      throw new ApiException(ErrorCode.FORBIDDEN);
    }
    MaintenanceRequest request = new MaintenanceRequest();
    request.setProperty(property);
    request.setCreatedBy(user);
    request.setTitle(createRequest.title());
    request.setDescription(createRequest.description());
    request.setStatus(MaintenanceStatus.OPEN);
    requests.save(request);
    return MaintenanceResponse.from(request);
  }

  @Transactional
  public MaintenanceResponse update(Long id, MaintenanceUpdateRequest updateRequest) {
    MaintenanceRequest request = find(id);
    MaintenanceStatus current = request.getStatus();
    MaintenanceStatus target = updateRequest.status();
    if (current != target
        && !ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
      throw new ApiException(ErrorCode.INVALID_STATE_TRANSITION);
    }
    request.setStatus(target);
    if (updateRequest.assignedTo() != null) {
      request.setAssignedTo(updateRequest.assignedTo());
    }
    return MaintenanceResponse.from(request);
  }

  private MaintenanceRequest find(Long id) {
    return requests.findById(id).orElseThrow(() -> new ApiException(ErrorCode.MAINTENANCE_NOT_FOUND));
  }

  private boolean isCurrentlyRenting(User user, Property property) {
    Tenant tenant = tenants.findByUserId(user.getId()).orElse(null);
    return tenant != null && contracts.existsByPropertyIdAndTenantIdAndStatus(
        property.getId(), tenant.getId(), ContractStatus.ACTIVE);
  }

  private static boolean canRead(MaintenanceRequest request, User user) {
    return switch (user.getRole()) {
      case ADMIN -> true;
      case OWNER -> request.getProperty().getOwner().getUser() != null
          && user.getId().equals(request.getProperty().getOwner().getUser().getId());
      case TENANT -> user.getId().equals(request.getCreatedBy().getId());
    };
  }
}
