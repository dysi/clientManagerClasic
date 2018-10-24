package com.dysi.AngryBoxClientManager.dtos;

import java.util.Calendar;

public class PaymentDTO {
	
	private Long id;
	
	private Long licenseId;
	
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
    
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getLicenseId() {
		return licenseId;
	}
	public void setLicenseId(Long licenseId) {
		this.licenseId = licenseId;
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
	public Calendar getPaymentRealDate() {
		return paymentRealDate;
	}
	public void setPaymentRealDate(Calendar paymentRealDate) {
		this.paymentRealDate = paymentRealDate;
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
	public String getObservation() {
		return observation;
	}
	public void setObservation(String observation) {
		this.observation = observation;
	}
	
}
