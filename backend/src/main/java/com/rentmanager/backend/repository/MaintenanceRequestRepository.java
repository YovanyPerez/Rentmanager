package com.rentmanager.backend.repository;

import com.rentmanager.backend.domain.MaintenanceRequest;
import com.rentmanager.backend.domain.MaintenanceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {

  @Override
  @EntityGraph(attributePaths = {"property", "createdBy"})
  List<MaintenanceRequest> findAll();

  @Override
  @EntityGraph(attributePaths = {"property", "createdBy"})
  Optional<MaintenanceRequest> findById(Long id);

  @EntityGraph(attributePaths = {"property", "createdBy"})
  List<MaintenanceRequest> findAllByPropertyOwnerUserId(Long userId);

  @EntityGraph(attributePaths = {"property", "createdBy"})
  List<MaintenanceRequest> findAllByCreatedById(Long userId);

  long countByStatus(MaintenanceStatus status);
}
