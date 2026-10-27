package com.dysi.AngryBoxClientManager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import javax.persistence.*;
import javax.validation.constraints.*;

/** Evidencia de una acción de licencia confirmada junto con la aprobación del pago. */
@Entity
@Table(name = "PAYMENT_LICENSE_ACTION")
public class PaymentLicenseAction extends AuditableEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PAYMENT_ID", nullable = false) @JsonIgnore @NotNull
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "LICENSE_ID", nullable = false) @JsonIgnore @NotNull
    private License license;

    @NotNull @Enumerated(EnumType.STRING) @Column(name = "ACTION_TYPE", length = 32, nullable = false)
    private LicenseActionType actionType;

    @Column(name = "PREVIOUS_VALUE", length = 100, nullable = true) @Size(max = 100)
    private String previousValue;

    @NotBlank @Column(name = "NEW_VALUE", length = 100, nullable = false) @Size(max = 100)
    private String newValue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RESPONSIBLE_USER_ID", nullable = false) @JsonIgnore @NotNull
    private User responsibleUser;

    @NotNull @Column(name = "OCCURRED_AT", nullable = false)
    private LocalDateTime occurredAt;

    public Long getId() { return id; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public License getLicense() { return license; }
    public void setLicense(License license) { this.license = license; }
    public LicenseActionType getActionType() { return actionType; }
    public void setActionType(LicenseActionType actionType) { this.actionType = actionType; }
    public String getPreviousValue() { return previousValue; }
    public void setPreviousValue(String previousValue) { this.previousValue = previousValue; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public User getResponsibleUser() { return responsibleUser; }
    public void setResponsibleUser(User responsibleUser) { this.responsibleUser = responsibleUser; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    @PrePersist
    private void validateApproval() {
        if (payment == null || payment.getStatus() != PaymentStatus.APROBADO || license == null
                || license.getClient() == null || payment.getClient() == null
                || !java.util.Objects.equals(license.getClient().getId(), payment.getClient().getId()))
            throw new IllegalStateException("La acción requiere un pago aprobado del mismo cliente.");
    }
    /** La evidencia se agrega; no se reemplaza ni borra de forma silenciosa. */
    @PreUpdate @PreRemove
    private void preserveEvidence() { throw new IllegalStateException("La evidencia es inmutable."); }
}
