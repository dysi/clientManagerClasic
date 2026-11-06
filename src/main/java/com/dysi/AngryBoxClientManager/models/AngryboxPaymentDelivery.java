package com.dysi.AngryBoxClientManager.models;

import java.time.Instant;
import javax.persistence.*;

/** Cola de aprobados: confirmar la entrega no altera el estado comercial del pago. */
@Entity
@Table(name = "angrybox_payment_delivery", indexes = @Index(name = "ix_payment_delivery_pending", columnList = "delivered_at,payment_id"))
public class AngryboxPaymentDelivery extends AuditableEntity {
    @Id
    @Column(name = "payment_id")
    private Long paymentId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "delivered_at")
    private Instant deliveredAt;
    @Column(name = "company_id")
    private Long companyId;
    @Column(name = "movement_id")
    private Long movementId;
    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    public Long getPaymentId() { return paymentId; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public Long getCompanyId() { return companyId; }
    public Long getMovementId() { return movementId; }
    public String getPayloadHash() { return payloadHash; }

    public void confirm(Long companyId, Long movementId, String hash) {
        this.companyId = companyId;
        this.movementId = movementId;
        this.payloadHash = hash;
        this.deliveredAt = Instant.now();
    }
}
