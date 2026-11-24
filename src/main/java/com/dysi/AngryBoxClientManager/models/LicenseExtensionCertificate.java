package com.dysi.AngryBoxClientManager.models;

import java.time.Instant;
import javax.persistence.*;

/** Extensión temporal por cliente; nunca modifica el vencimiento de las licencias. */
@Entity
@Table(name = "LICENSE_EXTENSION_CERTIFICATE", indexes = {
    @Index(name = "ix_extension_client_latest", columnList = "CLIENT_ID, ID"),
    @Index(name = "ix_extension_client_start", columnList = "CLIENT_ID, STARTS_AT, ID")
})
public class LicenseExtensionCertificate extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CLIENT_ID", nullable = false)
    private Client client;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PAYMENT_ID", unique = true)
    private Payment payment;
    @Enumerated(EnumType.STRING)
    @Column(name = "ORIGIN", nullable = false, length = 24)
    private Origin origin = Origin.PAYMENT;

    public enum Origin { PAYMENT, CLIENT_REQUEST }
    @Column(name = "STARTS_AT", nullable = false)
    private Instant startsAt;
    @Column(name = "EXPIRES_AT", nullable = false)
    private Instant expiresAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RECOVERED_BY_PAYMENT_ID")
    private Payment recoveredByPayment;

    public Long getId() { return id; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public Origin getOrigin() { return origin; }
    public void setOrigin(Origin origin) { this.origin = origin; }
    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Payment getRecoveredByPayment() { return recoveredByPayment; }
    public void setRecoveredByPayment(Payment payment) { this.recoveredByPayment = payment; }
}
