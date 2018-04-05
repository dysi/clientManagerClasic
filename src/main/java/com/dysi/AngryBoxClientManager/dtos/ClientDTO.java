package com.dysi.AngryBoxClientManager.dtos;

public class ClientDTO {
	
	private Long id;
	
	private Long idOld;

    private String firstname;
    private String lastname;
    private String email;
    private String phone;
    private String cuil;
    
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
	
}
