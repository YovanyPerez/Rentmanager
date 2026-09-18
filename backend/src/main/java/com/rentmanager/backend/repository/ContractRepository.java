package com.rentmanager.backend.repository;

import com.rentmanager.backend.domain.Contract;
import com.rentmanager.backend.domain.ContractStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, Long> {

  List<Contract> findAllByPropertyOwnerUserId(Long userId);

  List<Contract> findAllByTenantUserId(Long userId);

  Optional<Contract> findByIdAndPropertyOwnerUserId(Long id, Long userId);

  Optional<Contract> findByIdAndTenantUserId(Long id, Long userId);

  List<Contract> findAllByStatusAndEndDateBefore(ContractStatus status, LocalDate date);

  boolean existsByPropertyIdAndStatus(Long propertyId, ContractStatus status);

  boolean existsByPropertyIdAndTenantIdAndStatus(Long propertyId, Long tenantId, ContractStatus status);

  boolean existsByTenantId(Long tenantId);

  long countByStatusAndEndDateGreaterThanEqual(ContractStatus status, LocalDate date);
}
