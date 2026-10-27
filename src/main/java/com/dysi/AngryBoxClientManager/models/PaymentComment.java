package com.dysi.AngryBoxClientManager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;
import javax.validation.constraints.*;

/** Vínculo explícito del comentario con un pago, sin cascadas que destruyan evidencia. */
@Entity
@Table(name = "PAYMENT_COMMENT")
public class PaymentComment extends AuditableEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PAYMENT_ID", nullable = false) @JsonIgnore @NotNull
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "COMMENT_ID", nullable = false, unique = true) @JsonIgnore @NotNull
    private Comment comment;

    public Long getId() { return id; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public Comment getComment() { return comment; }
    public void setComment(Comment comment) { this.comment = comment; }
    /** La evidencia se agrega; no se reemplaza ni borra de forma silenciosa. */
    @PreUpdate @PreRemove
    private void preserveEvidence() { throw new IllegalStateException("La evidencia es inmutable."); }
}
