package com.rentmanager.backend.repository;

import com.rentmanager.backend.domain.Payment;
import com.rentmanager.backend.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

  long countByContractId(Long contractId);

  boolean existsByContractIdAndDueDate(Long contractId, LocalDate dueDate);

  List<Payment> findAllByStatusAndDueDateBefore(PaymentStatus status, LocalDate date);

  List<Payment> findAllByContractPropertyOwnerUserId(Long userId);

  List<Payment> findAllByContractTenantUserId(Long userId);

  Optional<Payment> findByIdAndContractPropertyOwnerUserId(Long id, Long userId);

  Optional<Payment> findByIdAndContractTenantUserId(Long id, Long userId);

  long countByStatus(PaymentStatus status);

  long countByStatusAndDueDateGreaterThanEqual(PaymentStatus status, LocalDate date);

  long countByStatusAndDueDateBefore(PaymentStatus status, LocalDate date);

  @Query("select sum(p.amount) from Payment p where p.status = ?1 and p.paidDate between ?2 and ?3")
  BigDecimal sumPaidBetween(PaymentStatus status, LocalDate from, LocalDate to);
}
