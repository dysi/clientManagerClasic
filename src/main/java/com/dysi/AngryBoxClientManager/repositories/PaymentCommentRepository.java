package com.dysi.AngryBoxClientManager.repositories;

import com.dysi.AngryBoxClientManager.models.PaymentComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentCommentRepository extends JpaRepository<PaymentComment, Long> {
    @Query("select pc.payment.id, count(pc) from PaymentComment pc join pc.comment c where pc.payment.id in :ids "
            + "and pc.removeDate is null and c.removeDate is null "
            + "and c.visibility = com.dysi.AngryBoxClientManager.models.CommentVisibility.PUBLICO group by pc.payment.id")
    java.util.List<Object[]> publicCounts(@Param("ids") java.util.List<Long> ids);

    @Query(value = "select pc from PaymentComment pc join fetch pc.comment c where pc.payment.id = :paymentId "
            + "and pc.payment.client.id = :clientId and pc.removeDate is null and c.removeDate is null "
            + "and c.visibility = com.dysi.AngryBoxClientManager.models.CommentVisibility.PUBLICO",
            countQuery = "select count(pc) from PaymentComment pc join pc.comment c where pc.payment.id = :paymentId "
            + "and pc.payment.client.id = :clientId and pc.removeDate is null and c.removeDate is null "
            + "and c.visibility = com.dysi.AngryBoxClientManager.models.CommentVisibility.PUBLICO")
    Page<PaymentComment> publicComments(@Param("paymentId") Long paymentId, @Param("clientId") Long clientId, Pageable page);
}
