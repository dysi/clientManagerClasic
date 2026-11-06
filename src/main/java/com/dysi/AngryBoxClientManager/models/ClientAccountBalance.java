package com.dysi.AngryBoxClientManager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.*;
import javax.validation.constraints.*;

/** Fotografía externa del saldo: positivo es deuda y negativo es saldo a favor. No se crea por defecto. */
@Entity
@Table(name = "CLIENT_ACCOUNT_BALANCE")
public class ClientAccountBalance extends AuditableEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "CLIENT_ID", nullable = false, unique = true) @NotNull @JsonIgnore
    private Client client;

    @NotNull @Digits(integer = 12, fraction = 2) @Column(name = "AMOUNT", precision = 14, scale = 2, nullable = false)
    private BigDecimal amount;

    @NotNull @Enumerated(EnumType.STRING) @Column(name = "CURRENCY", length = 32, nullable = false)
    private Currency currency = Currency.ARS;

    @NotNull @Column(name = "SYNCHRONIZED_AT", nullable = false)
    private LocalDateTime synchronizedAt;

    public Long getId() { return id; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }
    public LocalDateTime getSynchronizedAt() { return synchronizedAt; }
    public void setSynchronizedAt(LocalDateTime synchronizedAt) { this.synchronizedAt = synchronizedAt; }
}
