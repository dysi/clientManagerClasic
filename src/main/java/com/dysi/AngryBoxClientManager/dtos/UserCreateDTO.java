package com.dysi.AngryBoxClientManager.dtos;

import java.util.List;

public class UserCreateDTO {
	
	public UserCreateDTO() {
		super();
	}
	
    public UserCreateDTO(Long id, String username, String firstname, String lastname) {
		super();
		this.id = id;
		this.username = username;
		this.firstname = firstname;
		this.lastname = lastname;
	}

	private Long id;
    private String username;
    private String password;
    private String firstname;
    private String lastname;
    private String email;
    private Boolean enabled;
    private List<Long> authorities;
    
    private Long clientID;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<Long> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<Long> authorities) {
        this.authorities = authorities;
    }

	public Long getClientID() {
		return clientID;
	}

	public void setClientID(Long clientID) {
		this.clientID = clientID;
	}
    
}
