package com.dysi.AngryBoxClientManager.services;

import com.dysi.AngryBoxClientManager.dtos.LicensePaymentsPaginationDTO;
import com.dysi.AngryBoxClientManager.models.License;


public interface PaymentService {
	
	public LicensePaymentsPaginationDTO getLicensePaymentsPaginated(License license, int pageSize, int pageNumber);

}
