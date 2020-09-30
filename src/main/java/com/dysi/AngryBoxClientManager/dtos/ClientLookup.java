package com.dysi.AngryBoxClientManager.dtos;

import java.util.Calendar;

import com.dysi.AngryBoxClientManager.utils.FechasHelper;

public class ClientLookup {
	
	public ClientLookup() {}
	
	// CONSTRUCTOR MANUAL
	public ClientLookup(Long id, String firstname, String lastname, String email, String phone, String cuil,
			String username, String description, String companyName, Long companyId, String initialActivity,
			String lockDate, boolean prePayment, String block) {
		super();
		this.id = id;
		this.firstname = firstname;
		this.lastname = lastname;
		this.email = email;
		this.phone = phone;
		this.cuil = cuil;
		this.username = username;
		this.description = description;
		this.companyName = companyName;
		this.companyId = companyId;
		this.initialActivity = initialActivity;
		this.lockDate = lockDate;
		this.prePayment = prePayment;
		this.block = block;
	}
	
	// CONSTRUCTOR AUTOMATICO
	public ClientLookup(Long id, String firstname, String lastname, String username, String companyName, Long companyId, Calendar initialActivity,
			Calendar lockDateC, Boolean forcedLock) {
		super();
		this.id = id;
		
		this.firstname = firstname;
		this.lastname = lastname;
		this.username = (username != null ? username : "");
		
		// LICENCIA
		this.companyName = (companyName != null ? companyName : "");
		this.companyId = (companyId != null ? companyId : -1l);
		this.initialActivity = FechasHelper.getStringFromCalendarForSimplePrint(initialActivity);
		
		String lockDate = "NO IDENTIFICADO";
		
		lockDate = FechasHelper.getStringFromCalendarForSimplePrint(lockDateC);
		
		forcedLock = (forcedLock != null ? forcedLock : false);
		this.block = (forcedLock ? "BLOQUEADO" : ((lockDateC != null && lockDateC.getTimeInMillis() <= Calendar.getInstance().getTimeInMillis()) ? "VENCIDA" : ""));
		
		this.diasBlock = FechasHelper.getDiferenciaDias(Calendar.getInstance(), lockDateC);
		
		this.lockDate = lockDate;
		
	}
	
	// CLIENTE
	private Long id;
	
	private Long idOld;
	
    private String firstname;
    private String lastname;
    private String email;
    private String phone;
    private String cuil;
    
    private String username;
    
    // LICENCIA
    private String description;
    private String companyName;
    private Long companyId;
    
    // inicio de actividad
    private String initialActivity;
    // fecha de Bloqueo
    private String lockDate;
    // pago por adelantado
    private boolean prePayment;
    // BLOQUEADO
    private String block;
    private double diasBlock;
    
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
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
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
	public String getInitialActivity() {
		return initialActivity;
	}
	public void setInitialActivity(String initialActivity) {
		this.initialActivity = initialActivity;
	}
	public String getLockDate() {
		return lockDate;
	}
	public void setLockDate(String lockDate) {
		this.lockDate = lockDate;
	}
	public boolean isPrePayment() {
		return prePayment;
	}
	public void setPrePayment(boolean prePayment) {
		this.prePayment = prePayment;
	}
	public String getBlock() {
		return block;
	}
	public void setBlock(String block) {
		this.block = block;
	}
	public Long getIdOld() {
		return idOld;
	}
	public void setIdOld(Long idOld) {
		this.idOld = idOld;
	}

	public double getDiasBlock() {
		return diasBlock;
	}

	public void setDiasBlock(double diasBlock) {
		this.diasBlock = diasBlock;
	}
	
}
