package com.dysi.AngryBoxClientManager.models;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

@Entity
@Table(name = "CLIENT")
public class Client extends AuditableEntity {
	
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "FIRSTNAME")
    private String firstname;
    
    @Column(name = "LASTNAME")
    private String lastname;
    
    @Column(name = "EMAIL")
    private String email;
    
    @Column(name = "PHONE")
    private String phone;
    
    @Column(name = "CUIL")
    private String cuil;

    // DNI explícito; CUIL conserva el dato histórico sin clasificar ni normalizar a ciegas.
    @Column(name = "DNI", length = 8)
    @javax.validation.constraints.Pattern(regexp = "[0-9]{1,8}")
    private String dni;

    @Column(name = "LEGAL_NAME", length = 255)
    @javax.validation.constraints.Size(max = 255)
    private String legalName;

    @Column(name = "TRADE_NAME", length = 255)
    @javax.validation.constraints.Size(max = 255)
    private String tradeName;

    @Column(name = "EXTERNAL_CODE", length = 100)
    @javax.validation.constraints.Size(max = 100)
    private String externalCode;

    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = optionalText(legalName); }
    public String getTradeName() { return tradeName; }
    public void setTradeName(String tradeName) { this.tradeName = optionalText(tradeName); }
    public String getExternalCode() { return externalCode; }
    public void setExternalCode(String externalCode) { this.externalCode = optionalText(externalCode); }

    private static String optionalText(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    @OneToOne
    private User user;

    @OneToMany(mappedBy = "client")
    private List<License> licenses;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCuil() {
        return cuil;
    }

    public void setCuil(String cuil) {
        this.cuil = cuil;
    }

    @JsonIgnore
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @JsonIgnore
    public List<License> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<License> licenses) {
        this.licenses = licenses;
    }
}
