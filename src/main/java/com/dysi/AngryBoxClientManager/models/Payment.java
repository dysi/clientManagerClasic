package com.dysi.AngryBoxClientManager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.Calendar;

@Entity
public class Payment {
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // descuento
    private Double discount;
    // fecha de baja
    private Calendar removeDate;
    // fecha de creacion
    private Calendar creationDate;
    //fecha de pago
    private Calendar paymentDate;
  //fecha en que se pago
    private Calendar paymentRealDate;
    // fecha de vencimiento
    private Calendar expirationDate;
    // importe
    private Double amount;
    //observacion
    private String observation;

    @ManyToOne
    private License license;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Calendar getRemoveDate() {
        return removeDate;
    }

    public void setRemoveDate(Calendar removeDate) {
        this.removeDate = removeDate;
    }

    public Calendar getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Calendar creationDate) {
        this.creationDate = creationDate;
    }

    public Calendar getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Calendar paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Calendar getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Calendar expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    @JsonIgnore
    public License getLicense() {
        return license;
    }

    public void setLicense(License license) {
        this.license = license;
    }

	public Calendar getPaymentRealDate() {
		return paymentRealDate;
	}

	public void setPaymentRealDate(Calendar paymentRealDate) {
		this.paymentRealDate = paymentRealDate;
	}

	public String getObservation() {
		return observation;
	}

	public void setObservation(String observation) {
		this.observation = observation;
	}
	
}
