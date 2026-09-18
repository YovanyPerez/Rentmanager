package com.rentmanager.backend.repository;

import com.rentmanager.backend.domain.Tenant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

  boolean existsByUserId(Long userId);

  Optional<Tenant> findByUserId(Long userId);
}
