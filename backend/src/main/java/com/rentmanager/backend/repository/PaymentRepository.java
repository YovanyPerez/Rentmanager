package com.rentmanager.backend.repository;

import com.rentmanager.backend.domain.Payment;
import com.rentmanager.backend.domain.PaymentStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

  @Override
  @EntityGraph(attributePaths = {"contract", "contract.property", "contract.tenant"})
  List<Payment> findAll();

  @Override
  @EntityGraph(attributePaths = {"contract", "contract.property", "contract.tenant"})
  Optional<Payment> findById(Long id);

  @EntityGraph(attributePaths = {"contract", "contract.property", "contract.tenant"})
  List<Payment> findAllByContractPropertyOwnerUserId(Long userId);

  @EntityGraph(attributePaths = {"contract", "contract.property", "contract.tenant"})
  List<Payment> findAllByContractTenantUserId(Long userId);

  long countByContractId(Long contractId);

  boolean existsByContractIdAndDueDate(Long contractId, LocalDate dueDate);

  List<Payment> findAllByStatusAndDueDateBefore(PaymentStatus status, LocalDate date);

  long countByStatus(PaymentStatus status);

  long countByStatusAndDueDateGreaterThanEqual(PaymentStatus status, LocalDate date);

  long countByStatusAndDueDateBefore(PaymentStatus status, LocalDate date);

  @Query("""
      select p.contract.currency as currency, sum(p.amount) as total
      from Payment p
      where p.status = ?1 and p.paidDate between ?2 and ?3
      group by p.contract.currency
      order by p.contract.currency
      """)
  List<CurrencyTotalView> sumPaidByCurrencyBetween(PaymentStatus status, LocalDate from, LocalDate to);
}
