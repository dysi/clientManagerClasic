package com.dysi.AngryBoxClientManager.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dysi.AngryBoxClientManager.models.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Payment> {
    long countByClientIdAndStatusAndRemoveDateIsNull(Long clientId,
            com.dysi.AngryBoxClientManager.models.PaymentStatus status);

    @Query(value = "select p from Payment p join fetch p.client where p.status = :status and p.removeDate is null order by p.creationDate, p.id",
            countQuery = "select count(p) from Payment p where p.status = :status and p.removeDate is null")
    Page<Payment> findPendingReminder(@Param("status") com.dysi.AngryBoxClientManager.models.PaymentStatus status, Pageable pageable);

    java.util.Optional<Payment> findByClientIdAndSubmissionKey(Long clientId, String submissionKey);
    java.util.Optional<Payment> findByIdAndClientIdAndRemoveDateIsNull(Long id, Long clientId);

	@Query("select p from Payment p inner join p.license l"
			+ " where l.id = :licenseId"
			+ " order by p.issuedAt desc")
	Page<Payment> findPaymentsPaginatedByLicense(@Param("licenseId") Long licenseId, Pageable pageable);

	@Query("select p "
			+ " from Payment p"
			+ " inner join p.license l"
			+ " where l.id = :licenseId"
			+ " order by p.issuedAt desc")
	List<Payment> findPaymentsPaginatedByLicense(
			@Param("licenseId") long licenseId);
}
