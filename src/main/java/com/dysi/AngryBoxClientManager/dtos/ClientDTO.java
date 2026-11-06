package com.dysi.AngryBoxClientManager.dtos;

public class ClientDTO {

    public ClientDTO(Long id, String firstname, String lastname, String email, String phone, String cuil,
            String username, String legalName, String tradeName, String externalCode) {
        this(id, firstname, lastname, email, phone, cuil, username);
        this.legalName = legalName; this.tradeName = tradeName; this.externalCode = externalCode;
    }
	
	public ClientDTO(Long id, String firstname, String lastname, String email, String phone, String cuil, String username) {
		super();
		this.id = id;
		this.firstname = firstname;
		this.lastname = lastname;
		this.email = email;
		this.phone = phone;
		this.cuil = cuil;
		this.username = (username != null ? username : "SIN ASIGNAR");
		this.portalUserMissing = username == null;
	}
	
	public ClientDTO() {
		super();
	}
	
	private Long id;
	
	private Long idOld;

    private String firstname;
    private String lastname;
    private String email;
    private String phone;
    private String cuil;
    private String dni;
    private String legalName;
    private String tradeName;
    private String externalCode;

    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }
    public String getTradeName() { return tradeName; }
    public void setTradeName(String tradeName) { this.tradeName = tradeName; }
    public String getExternalCode() { return externalCode; }
    public void setExternalCode(String externalCode) { this.externalCode = externalCode; }
    private boolean portalUserMissing;

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public boolean isPortalUserMissing() { return portalUserMissing; }
    
    private String username;
    
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getIdOld() {
		return idOld;
	}
	public void setIdOld(Long idOld) {
		this.idOld = idOld;
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
	
}
