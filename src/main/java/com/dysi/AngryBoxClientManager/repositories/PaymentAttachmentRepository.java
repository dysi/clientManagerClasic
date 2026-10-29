package com.dysi.AngryBoxClientManager.repositories;

import com.dysi.AngryBoxClientManager.models.PaymentAttachment;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PaymentAttachmentRepository extends JpaRepository<PaymentAttachment, Long> {
    @Query("select pa.payment.id, count(pa) from PaymentAttachment pa join pa.attachment a "
            + "where pa.payment.id in :ids and pa.removeDate is null and a.removeDate is null group by pa.payment.id")
    java.util.List<Object[]> activeCounts(@Param("ids") java.util.List<Long> ids);

    @Query(value = "select pa from PaymentAttachment pa join fetch pa.attachment a where pa.payment.id = :id "
            + "and pa.payment.client.id = :client and pa.payment.removeDate is null and pa.removeDate is null and a.removeDate is null",
            countQuery = "select count(pa) from PaymentAttachment pa join pa.attachment a where pa.payment.id = :id "
            + "and pa.payment.client.id = :client and pa.payment.removeDate is null and pa.removeDate is null and a.removeDate is null")
    Page<PaymentAttachment> ownReceipts(@Param("id") Long id, @Param("client") Long client, Pageable page);
}
