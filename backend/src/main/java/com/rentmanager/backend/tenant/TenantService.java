package com.rentmanager.backend.tenant;

import com.rentmanager.backend.domain.Role;
import com.rentmanager.backend.domain.Tenant;
import com.rentmanager.backend.domain.User;
import com.rentmanager.backend.error.ApiException;
import com.rentmanager.backend.error.ErrorCode;
import com.rentmanager.backend.repository.ContractRepository;
import com.rentmanager.backend.repository.TenantRepository;
import com.rentmanager.backend.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {

  private final TenantRepository tenants;
  private final UserRepository users;
  private final ContractRepository contracts;

  public TenantService(TenantRepository tenants, UserRepository users, ContractRepository contracts) {
    this.tenants = tenants;
    this.users = users;
    this.contracts = contracts;
  }

  @Transactional(readOnly = true)
  public List<TenantResponse> list() {
    return tenants.findAll().stream().map(TenantResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public TenantResponse get(Long id) {
    return TenantResponse.from(find(id));
  }

  @Transactional
  public TenantResponse create(TenantRequest request) {
    Tenant tenant = new Tenant();
    tenant.setFullName(request.fullName());
    tenant.setEmail(request.email());
    tenant.setPhone(request.phone());
    tenant.setUser(resolveUser(request.userId()));
    tenants.save(tenant);
    return TenantResponse.from(tenant);
  }

  @Transactional
  public TenantResponse update(Long id, TenantRequest request) {
    Tenant tenant = find(id);
    tenant.setFullName(request.fullName());
    tenant.setEmail(request.email());
    tenant.setPhone(request.phone());
    Long userId = request.userId();
    if (userId == null) {
      tenant.setUser(null);
    } else if (tenant.getUser() == null || !tenant.getUser().getId().equals(userId)) {
      tenant.setUser(resolveUser(userId));
    }
    return TenantResponse.from(tenant);
  }

  @Transactional
  public void delete(Long id) {
    Tenant tenant = find(id);
    if (contracts.existsByTenantId(id)) {
      throw new ApiException(ErrorCode.TENANT_HAS_CONTRACTS);
    }
    tenants.delete(tenant);
  }

  private Tenant find(Long id) {
    return tenants.findById(id).orElseThrow(() -> new ApiException(ErrorCode.TENANT_NOT_FOUND));
  }

  private User resolveUser(Long userId) {
    if (userId == null) {
      return null;
    }
    User user = users.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    if (user.getRole() != Role.TENANT) {
      throw new ApiException(ErrorCode.USER_ROLE_MISMATCH);
    }
    if (tenants.existsByUserId(userId)) {
      throw new ApiException(ErrorCode.USER_ALREADY_LINKED);
    }
    return user;
  }
}
