package com.dysi.AngryBoxClientManager.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@Table(name = "PAYMENT")
public class Payment extends AuditableEntity {
	
    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // descuento
    @Column(name = "DISCOUNT")
    private Double discount;
    //fecha de pago
    @Column(name = "PAYMENTDATE")
    private Calendar paymentDate;
    
 	//fecha en que se pago
    @Column(name = "PAYMENTREALDATE")
    private Calendar paymentRealDate;
    
    // fecha de vencimiento
    @Column(name = "EXPIRATIONDATE")
    private Calendar expirationDate;
    
    // importe
    @Column(name = "AMOUNT")
    private Double amount;
    
    //observacion
    @Column(name = "OBSERVATION")
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
