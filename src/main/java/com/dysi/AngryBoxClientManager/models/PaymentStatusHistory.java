package com.dysi.AngryBoxClientManager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import javax.persistence.*;
import javax.validation.constraints.*;

/** Evento inmutable de creación o decisión del pago; no sustituye la autorización del servicio. */
@Entity
@Table(name = "PAYMENT_STATUS_HISTORY", uniqueConstraints = @UniqueConstraint(name = "uk_payment_history_state", columnNames = { "PAYMENT_ID", "NEW_STATUS" }))
public class PaymentStatusHistory extends AuditableEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PAYMENT_ID", nullable = false) @JsonIgnore @NotNull
    private Payment payment;

    @Enumerated(EnumType.STRING) @Column(name = "PREVIOUS_STATUS", length = 32, updatable = false)
    private PaymentStatus previousStatus;

    @NotNull @Enumerated(EnumType.STRING) @Column(name = "NEW_STATUS", length = 32, nullable = false)
    private PaymentStatus newStatus;

    @NotNull @Column(name = "OCCURRED_AT", nullable = false)
    private LocalDateTime occurredAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RESPONSIBLE_USER_ID", nullable = false) @JsonIgnore @NotNull
    private User responsibleUser;

    @NotNull @Enumerated(EnumType.STRING) @Column(name = "ORIGIN", length = 32, nullable = false)
    private OperationOrigin origin;

    @Column(name = "REASON", length = 1000, nullable = true) @Size(max = 1000)
    private String reason;

    public Long getId() { return id; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public PaymentStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(PaymentStatus previousStatus) { this.previousStatus = previousStatus; }
    public PaymentStatus getNewStatus() { return newStatus; }
    public void setNewStatus(PaymentStatus newStatus) { this.newStatus = newStatus; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public User getResponsibleUser() { return responsibleUser; }
    public void setResponsibleUser(User responsibleUser) { this.responsibleUser = responsibleUser; }
    public OperationOrigin getOrigin() { return origin; }
    public void setOrigin(OperationOrigin origin) { this.origin = origin; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    @PrePersist
    private void validateTransition() {
        boolean creation = previousStatus == null && newStatus == PaymentStatus.PENDIENTE;
        boolean decision = previousStatus == PaymentStatus.PENDIENTE
                && (newStatus == PaymentStatus.APROBADO || newStatus == PaymentStatus.RECHAZADO);
        if (!creation && !decision) throw new IllegalStateException("Transición de pago inválida.");
        if (decision && origin != OperationOrigin.ACCESO_AUTENTICADO)
            throw new IllegalStateException("La decisión requiere acceso autenticado.");
        if (newStatus == PaymentStatus.RECHAZADO && (reason == null || reason.trim().isEmpty()))
            throw new IllegalStateException("El rechazo requiere un motivo público.");
    }
    /** La evidencia se agrega; no se reemplaza ni borra de forma silenciosa. */
    @PreUpdate @PreRemove
    private void preserveEvidence() { throw new IllegalStateException("La evidencia es inmutable."); }
}
