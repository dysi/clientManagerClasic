package com.dysi.AngryBoxClientManager.dtos;

import java.util.Calendar;

public class LicenseDTO {
	
	private Long id;
    private String description;
    private String companyName;
    private Long companyId;
    // inicio de actividad
    private Calendar initialActivity;
    // fecha de Bloqueo
    private Calendar lockDate;
    // pago por adelantado
    private boolean prePayment;
    
    private Long clienteId;
    
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
	public Long getClienteId() {
		return clienteId;
	}
	public void setClienteId(Long clienteId) {
		this.clienteId = clienteId;
	}
	public Calendar getLockDate() {
		return lockDate;
	}
	public void setLockDate(Calendar lockDate) {
		this.lockDate = lockDate;
	}
	
}
