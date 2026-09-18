package com.rentmanager.backend.repository;

import com.rentmanager.backend.domain.Contract;
import com.rentmanager.backend.domain.ContractStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, Long> {

  @Override
  @EntityGraph(attributePaths = {"property", "tenant"})
  List<Contract> findAll();

  @Override
  @EntityGraph(attributePaths = {"property", "tenant"})
  Optional<Contract> findById(Long id);

  @EntityGraph(attributePaths = {"property", "tenant"})
  List<Contract> findAllByPropertyOwnerUserId(Long userId);

  @EntityGraph(attributePaths = {"property", "tenant"})
  List<Contract> findAllByTenantUserId(Long userId);

  List<Contract> findAllByStatusAndEndDateBefore(ContractStatus status, LocalDate date);

  boolean existsByPropertyIdAndStatus(Long propertyId, ContractStatus status);

  boolean existsByPropertyIdAndTenantIdAndStatus(Long propertyId, Long tenantId, ContractStatus status);

  boolean existsByTenantId(Long tenantId);

  long countByStatusAndEndDateGreaterThanEqual(ContractStatus status, LocalDate date);
}
