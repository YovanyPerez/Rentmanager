package com.rentmanager.backend.owner;

import com.rentmanager.backend.domain.Owner;
import com.rentmanager.backend.domain.Role;
import com.rentmanager.backend.domain.User;
import com.rentmanager.backend.error.ApiException;
import com.rentmanager.backend.error.ErrorCode;
import com.rentmanager.backend.repository.OwnerRepository;
import com.rentmanager.backend.repository.PropertyRepository;
import com.rentmanager.backend.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OwnerService {

  private final OwnerRepository owners;
  private final UserRepository users;
  private final PropertyRepository properties;

  public OwnerService(OwnerRepository owners, UserRepository users, PropertyRepository properties) {
    this.owners = owners;
    this.users = users;
    this.properties = properties;
  }

  @Transactional(readOnly = true)
  public List<OwnerResponse> list() {
    return owners.findAll().stream().map(OwnerResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public OwnerResponse get(Long id) {
    return OwnerResponse.from(find(id));
  }

  @Transactional
  public OwnerResponse create(OwnerRequest request) {
    Owner owner = new Owner();
    owner.setFullName(request.fullName());
    owner.setEmail(request.email());
    owner.setPhone(request.phone());
    owner.setUser(resolveUser(request.userId()));
    owners.save(owner);
    return OwnerResponse.from(owner);
  }

  @Transactional
  public OwnerResponse update(Long id, OwnerRequest request) {
    Owner owner = find(id);
    owner.setFullName(request.fullName());
    owner.setEmail(request.email());
    owner.setPhone(request.phone());
    Long userId = request.userId();
    if (userId == null) {
      owner.setUser(null);
    } else if (owner.getUser() == null || !owner.getUser().getId().equals(userId)) {
      owner.setUser(resolveUser(userId));
    }
    return OwnerResponse.from(owner);
  }

  @Transactional
  public void delete(Long id) {
    Owner owner = find(id);
    if (properties.existsByOwnerId(id)) {
      throw new ApiException(ErrorCode.OWNER_HAS_PROPERTIES);
    }
    owners.delete(owner);
  }

  private Owner find(Long id) {
    return owners.findById(id).orElseThrow(() -> new ApiException(ErrorCode.OWNER_NOT_FOUND));
  }

  private User resolveUser(Long userId) {
    if (userId == null) {
      return null;
    }
    User user = users.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    if (user.getRole() != Role.OWNER) {
      throw new ApiException(ErrorCode.USER_ROLE_MISMATCH);
    }
    if (owners.existsByUserId(userId)) {
      throw new ApiException(ErrorCode.USER_ALREADY_LINKED);
    }
    return user;
  }
}
