package com.rentmanager.backend.repository;

import com.rentmanager.backend.domain.MaintenanceRequest;
import com.rentmanager.backend.domain.MaintenanceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {

  List<MaintenanceRequest> findAllByPropertyOwnerUserId(Long userId);

  List<MaintenanceRequest> findAllByCreatedById(Long userId);

  Optional<MaintenanceRequest> findByIdAndCreatedById(Long id, Long userId);

  long countByStatus(MaintenanceStatus status);
}
