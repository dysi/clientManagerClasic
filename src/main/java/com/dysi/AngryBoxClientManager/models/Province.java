package com.dysi.AngryBoxClientManager.models;
import javax.persistence.*;
import javax.validation.constraints.*;
/** Catálogo geográfico auditado, sin cascadas sobre clientes ni usuarios. */
@Entity @Table(name = "PROVINCE", uniqueConstraints = @UniqueConstraint(name = "uk_province_name", columnNames = {"COUNTRY_ID", "NAME"}))
public class Province extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "ID")
    private Long id;
    @NotBlank @Size(max = 100) @Column(name = "NAME", length = 100, nullable = false)
    private String name;
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "COUNTRY_ID", nullable = false)
    private Country country;
    public Country getCountry() { return country; }
    public void setCountry(Country country) { this.country = country; }
}
