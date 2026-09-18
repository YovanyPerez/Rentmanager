package com.rentmanager.backend.repository;

import com.rentmanager.backend.domain.PropertyImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {

  List<PropertyImage> findByPropertyIdOrderByPositionAscIdAsc(Long propertyId);

  List<PropertyImage> findByPropertyIdInOrderByPositionAscIdAsc(List<Long> propertyIds);

  long countByPropertyId(Long propertyId);
}
