package com.dysi.AngryBoxClientManager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;
import javax.validation.constraints.*;

/** Vínculo explícito del comprobante con un pago, sin cascadas de eliminación. */
@Entity
@Table(name = "PAYMENT_ATTACHMENT")
public class PaymentAttachment extends AuditableEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PAYMENT_ID", nullable = false) @JsonIgnore @NotNull
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ATTACHMENT_ID", nullable = false, unique = true) @JsonIgnore @NotNull
    private Attachment attachment;

    /** NULL identifica la presentación original; cada agregado tiene su clave de reintento. */
    @Size(max = 64) @Column(name = "UPLOAD_KEY", length = 64)
    private String uploadKey;

    public String getUploadKey() { return uploadKey; }
    public void setUploadKey(String uploadKey) { this.uploadKey = uploadKey; }
    public Long getId() { return id; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public Attachment getAttachment() { return attachment; }
    public void setAttachment(Attachment attachment) { this.attachment = attachment; }
    /** La evidencia se agrega; no se reemplaza ni borra de forma silenciosa. */
    @PreUpdate @PreRemove
    private void preserveEvidence() { throw new IllegalStateException("La evidencia es inmutable."); }
}
