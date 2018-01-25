package com.dysi.AngryBoxClientManager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.Calendar;
import java.util.List;

@Entity
public class License {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String description;

    private String companyName;
    private Long companyId;

    // inicio de actividad
    private Calendar initialActivity;
    // pago por adelantado
    private boolean prePayment;

    @ManyToOne
    private Client client;

    @OneToMany(mappedBy = "license")
    List<Payment> paymentList;

    @OneToMany(mappedBy = "license")
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
