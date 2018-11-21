package com.dysi.AngryBoxClientManager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.Calendar;
import java.util.List;

@Entity
@Table(name = "LICENSE")
public class License {
	
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "COMPANYNAME")
    private String companyName;
    
    @Column(name = "COMPANYID")
    private Long companyId;

    // inicio de actividad
    @Column(name = "INITIALACTIVITY")
    private Calendar initialActivity;
    
    // pago por adelantado
    @Column(name = "PREPAYMENT")
    private boolean prePayment;

    @ManyToOne
    @Column(name = "CLIENT")
    private Client client;

    @OneToMany(mappedBy = "license", cascade=CascadeType.ALL, orphanRemoval=true)
    @Column(name = "PAYMENTLIST")
    List<Payment> paymentList;

    @OneToMany(mappedBy = "license", cascade=CascadeType.ALL, orphanRemoval=true)
    @Column(name = "LICENSESTATUSES")
    List<LicenseStatus> licenseStatuses;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Calendar getInitialActivity() {
        return initialActivity;
    }

    public void setInitialActivity(Calendar initialActivity) {
        this.initialActivity = initialActivity;
    }

    public boolean isPrePayment() {
        return prePayment;
    }

    public void setPrePayment(boolean prePayment) {
        this.prePayment = prePayment;
    }

    @JsonIgnore
    public List<Payment> getPaymentList() {
        return paymentList;
    }

    public void setPaymentList(List<Payment> paymentList) {
        this.paymentList = paymentList;
    }

    public List<LicenseStatus> getLicenseStatuses() {
        return licenseStatuses;
    }

    public void setLicenseStatuses(List<LicenseStatus> licenseStatuses) {
        this.licenseStatuses = licenseStatuses;
    }

    @JsonIgnore
    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
