package com.dysi.AngryBoxClientManager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.*;
import javax.validation.constraints.*;

/** Presentación de transferencia de un cliente. Las escrituras HTTP antiguas permanecen cerradas. */
@Entity
@Table(name = "PAYMENT", uniqueConstraints = @UniqueConstraint(name = "uk_payment_submission", columnNames = { "CLIENT_ID", "SUBMISSION_KEY" }))
public class Payment extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CLIENT_ID", nullable = false) @JsonIgnore @NotNull
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "LICENSE_ID", nullable = true) @JsonIgnore
    private License license;

    @DecimalMin(value = "0.00", inclusive = false) @NotNull @Digits(integer = 12, fraction = 2) @Column(name = "AMOUNT", precision = 14, scale = 2, nullable = false)
    private BigDecimal amount;

    @NotNull @Enumerated(EnumType.STRING) @Column(name = "CURRENCY", length = 32, nullable = false)
    private Currency currency = Currency.ARS;

    @NotNull @Column(name = "ISSUED_AT", nullable = false)
    private LocalDateTime issuedAt;

    @NotBlank @Column(name = "CONTACT_PHONE", length = 50, nullable = false) @Size(max = 50)
    private String contactPhone;

    @NotNull @Enumerated(EnumType.STRING) @Column(name = "METHOD", length = 32, nullable = false)
    private PaymentMethod method = PaymentMethod.TRANSFERENCIA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "BANK_ACCOUNT_ID", nullable = false) @JsonIgnore @NotNull
    private BankAccount bankAccount;

    @NotBlank @Column(name = "DESTINATION_BANK", length = 255, nullable = false) @Size(max = 255)
    private String destinationBank;

    @NotBlank @Column(name = "DESTINATION_HOLDER", length = 255, nullable = false) @Size(max = 255)
    private String destinationHolder;

    @Pattern(regexp = "[0-9]{22}") @NotBlank @Column(name = "DESTINATION_CBU", length = 22, nullable = false) @Size(max = 22)
    private String destinationCbu;

    @Column(name = "DESTINATION_ALIAS", length = 100, nullable = true) @Size(max = 100)
    private String destinationAlias;

    @NotNull @Enumerated(EnumType.STRING) @Column(name = "STATUS", length = 32, nullable = false)
    private PaymentStatus status = PaymentStatus.PENDIENTE;

    @NotNull @Enumerated(EnumType.STRING) @Column(name = "ORIGIN", length = 32, nullable = false)
    private OperationOrigin origin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SUBMITTED_BY_ID", nullable = false) @JsonIgnore @NotNull
    private User submittedBy;

    @Column(name = "WITHOUT_RECEIPT", nullable = false)
    private boolean withoutReceipt = false;

    @Column(name = "NO_RECEIPT_REASON", length = 1000, nullable = true) @Size(max = 1000)
    private String noReceiptReason;

    @NotBlank @Column(name = "SUBMISSION_KEY", length = 64, nullable = false) @Size(max = 64)
    private String submissionKey;

    /** Huella anterior a la primera corrección: mantiene la idempotencia de la presentación original. */
    @JsonIgnore @Column(name = "ORIGINAL_EDITABLE_HASH", length = 64)
    private String originalEditableHash;

    /** Huella de cuenta y fotografía bancaria anterior a la primera corrección del destino. */
    @JsonIgnore @Column(name = "ORIGINAL_DESTINATION_HASH", length = 64)
    private String originalDestinationHash;

    public String getOriginalDestinationHash() { return originalDestinationHash; }
    public void preserveOriginalDestinationHash(String hash) {
        if (originalDestinationHash == null) originalDestinationHash = hash;
    }

    public String getOriginalEditableHash() { return originalEditableHash; }
    public void preserveOriginalEditableHash(String hash) {
        if (originalEditableHash == null) originalEditableHash = hash;
    }

    @Version @Column(name = "VERSION", nullable = false)
    private long version = 0;

    public Long getId() { return id; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public License getLicense() { return license; }
    public void setLicense(License license) { this.license = license; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }
    public BankAccount getBankAccount() { return bankAccount; }
    public void setBankAccount(BankAccount bankAccount) { this.bankAccount = bankAccount; }
    public String getDestinationBank() { return destinationBank; }
    public void setDestinationBank(String destinationBank) { this.destinationBank = destinationBank; }
    public String getDestinationHolder() { return destinationHolder; }
    public void setDestinationHolder(String destinationHolder) { this.destinationHolder = destinationHolder; }
    public String getDestinationCbu() { return destinationCbu; }
    public void setDestinationCbu(String destinationCbu) { this.destinationCbu = destinationCbu; }
    public String getDestinationAlias() { return destinationAlias; }
    public void setDestinationAlias(String destinationAlias) { this.destinationAlias = destinationAlias; }
    /** Decisión administrativa que admite rectificaciones sin reabrir a pendiente. */
    public void decide(PaymentStatus next) {
        if (status == next || (next != PaymentStatus.APROBADO && next != PaymentStatus.RECHAZADO))
            throw new IllegalStateException("La transición del pago no está permitida.");
        status = next;
    }
    public PaymentStatus getStatus() { return status; }
    public OperationOrigin getOrigin() { return origin; }
    public void setOrigin(OperationOrigin origin) { this.origin = origin; }
    public User getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(User submittedBy) { this.submittedBy = submittedBy; }
    public boolean getWithoutReceipt() { return withoutReceipt; }
    public void setWithoutReceipt(boolean withoutReceipt) { this.withoutReceipt = withoutReceipt; }
    public String getNoReceiptReason() { return noReceiptReason; }
    public void setNoReceiptReason(String noReceiptReason) { this.noReceiptReason = noReceiptReason; }
    public String getSubmissionKey() { return submissionKey; }
    public void setSubmissionKey(String submissionKey) { this.submissionKey = submissionKey; }
    public long getVersion() { return version; }

    @PreRemove
    private void preservePayment() { throw new IllegalStateException("Los pagos no admiten borrado ordinario."); }

    /** Defensa adicional a la FK compuesta MySQL; nunca inferir el cliente desde el navegador. */
    @PrePersist @PreUpdate
    private void validateRelations() {
        if (method != PaymentMethod.TRANSFERENCIA) throw new IllegalStateException("QR todavía no está disponible.");
        if (license != null && (client == null || license.getClient() == null || !java.util.Objects.equals(client.getId(), license.getClient().getId())))
            throw new IllegalStateException("La licencia no pertenece al cliente del pago.");
        if (withoutReceipt && (noReceiptReason == null || noReceiptReason.trim().isEmpty()))
            throw new IllegalStateException("Completá el motivo de ausencia de comprobante.");
        if (!withoutReceipt && noReceiptReason != null && !noReceiptReason.trim().isEmpty())
            throw new IllegalStateException("No corresponde un motivo de ausencia con comprobantes.");
    }
}
