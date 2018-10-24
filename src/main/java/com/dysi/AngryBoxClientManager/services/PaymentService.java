package com.dysi.AngryBoxClientManager.services;

import java.util.List;

import com.dysi.AngryBoxClientManager.dtos.LicensePaymentsPaginationDTO;
import com.dysi.AngryBoxClientManager.dtos.PaymentDTO;
import com.dysi.AngryBoxClientManager.models.License;
import com.dysi.AngryBoxClientManager.models.Payment;


public interface PaymentService {
	
	public LicensePaymentsPaginationDTO getLicensePaymentsPaginated(License license, int pageSize, int pageNumber);
	public List<Payment> getLicensePayments(long licenseId);
	public String editPayment(PaymentDTO payment);
	public String pagoBaja(Long pagoID);

}
