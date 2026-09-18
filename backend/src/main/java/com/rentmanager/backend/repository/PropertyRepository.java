package com.rentmanager.backend.repository;

import com.rentmanager.backend.domain.Property;
import com.rentmanager.backend.domain.PropertyStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PropertyRepository extends JpaRepository<Property, Long> {

  List<Property> findAllByOwnerUserId(Long userId);

  Optional<Property> findByIdAndOwnerUserId(Long id, Long userId);

  boolean existsByOwnerId(Long ownerId);

  long countByStatus(PropertyStatus status);

  @Query("""
      select p from Property p
      where p.status = :status
        and (:query = '' or lower(p.city) like lower(concat('%', :query, '%'))
             or lower(p.address) like lower(concat('%', :query, '%')))
      order by p.monthlyRent asc
      """)
  List<Property> searchAvailable(@Param("status") PropertyStatus status, @Param("query") String query);
}
