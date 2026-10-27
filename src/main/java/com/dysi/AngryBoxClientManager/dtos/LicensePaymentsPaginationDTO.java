package com.dysi.AngryBoxClientManager.dtos;

import org.springframework.data.domain.Page;

import com.dysi.AngryBoxClientManager.dtos.AdminViews.PaymentView;

public class LicensePaymentsPaginationDTO {
	
	private Long licenseId;
	private String description;
	private String companyName;
	private Long companyId;

	Page<PaymentView> payments;

	public Long getLicenseId() {
		return licenseId;
	}

	public void setLicenseId(Long licenseId) {
		this.licenseId = licenseId;
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

	public Page<PaymentView> getPayments() {
		return payments;
	}

	public void setPayments(Page<PaymentView> payments) {
		this.payments = payments;
	}

}
