package com.dysi.AngryBoxClientManager.models;
import javax.persistence.*;
import javax.validation.constraints.*;
/** Catálogo geográfico auditado, sin cascadas sobre clientes ni usuarios. */
@Entity @Table(name = "COUNTRY", uniqueConstraints = @UniqueConstraint(name = "uk_country_name", columnNames = "NAME"))
public class Country extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID")
    private Long id;
    @NotBlank @Size(max = 100) @Column(name = "NAME", length = 100, nullable = false)
    private String name;
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
