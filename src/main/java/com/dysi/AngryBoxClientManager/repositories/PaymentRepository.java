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

	@Query("select p from Payment p inner join p.license l"
			+ " where l.id = :licenseId"
			+ " order by p.paymentDate desc")
	Page<Payment> findPaymentsPaginatedByLicense(@Param("licenseId") Long licenseId, Pageable pageable);

	@Query("select p "
			+ " from Payment p"
			+ " inner join p.license l"
			+ " where l.id = :licenseId"
			+ " order by p.paymentDate desc")
	List<Payment> findPaymentsPaginatedByLicense(
			@Param("licenseId") long licenseId);
}
