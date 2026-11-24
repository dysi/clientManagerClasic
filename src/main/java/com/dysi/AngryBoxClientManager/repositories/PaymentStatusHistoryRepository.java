package com.dysi.AngryBoxClientManager.repositories;

import com.dysi.AngryBoxClientManager.models.PaymentStatus;
import com.dysi.AngryBoxClientManager.models.PaymentStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentStatusHistoryRepository extends JpaRepository<PaymentStatusHistory, Long> {
    /** Último evento de ese estado por pago, también cuando hubo varias rectificaciones. */
    @Query("select h from PaymentStatusHistory h where h.payment.id in :ids "
            + "and h.newStatus = :status and h.id = (select max(t.id) from PaymentStatusHistory t "
            + "where t.payment = h.payment and t.newStatus = :status)")
    List<PaymentStatusHistory> findByPaymentIdInAndNewStatus(@Param("ids") List<Long> ids,
            @Param("status") PaymentStatus status);
}
