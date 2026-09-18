package com.rentmanager.backend.repository;

import com.rentmanager.backend.domain.Owner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerRepository extends JpaRepository<Owner, Long> {

  boolean existsByUserId(Long userId);
}
