package com.dysi.AngryBoxClientManager.models;

import javax.persistence.*;
import javax.validation.constraints.*;

/** Banco parametrizable; la clave normalizada evita variantes de nombre por espacios o mayúsculas. */
@Entity
@Table(name = "BANK", uniqueConstraints = @UniqueConstraint(name = "uk_bank_name", columnNames = "NORMALIZED_NAME"))
public class Bank extends AuditableEntity {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank @Column(name = "NAME", length = 255, nullable = false) @Size(max = 255)
    private String name;

    @NotBlank @Column(name = "NORMALIZED_NAME", length = 255, nullable = false) @Size(max = 255)
    private String normalizedName;

    @Column(name = "ACTIVE", nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) {
        this.name = name == null ? null : name.trim().replaceAll("\\s+", " ");
        this.normalizedName = this.name == null ? null : this.name.toLowerCase(java.util.Locale.ROOT);
    }
    public String getNormalizedName() { return normalizedName; }
    public boolean getActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @PrePersist @PreUpdate
    private void normalize() {
        setName(name);
    }
}
