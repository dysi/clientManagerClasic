package com.dysi.AngryBoxClientManager.repositories;

import com.dysi.AngryBoxClientManager.models.PaymentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentStatusHistoryRepository extends JpaRepository<PaymentStatusHistory, Long> {
    java.util.List<PaymentStatusHistory> findByPaymentIdInAndNewStatus(java.util.List<Long> ids,
            com.dysi.AngryBoxClientManager.models.PaymentStatus status);
}
