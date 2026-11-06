package com.dysi.AngryBoxClientManager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.*;
import javax.validation.constraints.*;

/** Cuenta destinataria desactivable; los pagos conservan además una fotografía independiente. */
@Entity
@Table(name = "BANK_ACCOUNT", uniqueConstraints = { @UniqueConstraint(name = "uk_bank_account_cbu", columnNames = "CBU"), @UniqueConstraint(name = "uk_bank_account_alias", columnNames = "ALIAS") })
public class BankAccount extends AuditableEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "BANK_ID", nullable = false) @JsonIgnore @NotNull
    private Bank bank;

    @NotBlank @Column(name = "HOLDER", length = 255, nullable = false) @Size(max = 255)
    private String holder;

    @Pattern(regexp = "[0-9]{22}") @NotBlank @Column(name = "CBU", length = 22, nullable = false) @Size(max = 22)
    private String cbu;

    @Column(name = "ALIAS", length = 100, nullable = true) @Size(max = 100)
    private String alias;

    @Column(name = "ACTIVE", nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public Bank getBank() { return bank; }
    public void setBank(Bank bank) { this.bank = bank; }
    public String getHolder() { return holder; }
    public void setHolder(String holder) { this.holder = holder == null ? null : holder.trim(); }
    public String getCbu() { return cbu; }
    public void setCbu(String cbu) { this.cbu = cbu == null ? null : cbu.replaceAll("[\\s-]", ""); }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias == null || alias.trim().isEmpty() ? null : alias.trim().toLowerCase(java.util.Locale.ROOT); }
    public boolean getActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @PrePersist @PreUpdate
    private void normalize() {
        if (holder != null) holder = holder.trim();
        if (cbu != null) cbu = cbu.replaceAll("[\\s-]", "");
        if (alias != null) { alias = alias.trim().toLowerCase(java.util.Locale.ROOT); if (alias.isEmpty()) alias = null; }
    }
}
