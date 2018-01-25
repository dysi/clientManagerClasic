package com.dysi.AngryBoxClientManager.servicesImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.dysi.AngryBoxClientManager.dtos.LicensePaymentsPaginationDTO;
import com.dysi.AngryBoxClientManager.models.License;
import com.dysi.AngryBoxClientManager.models.Payment;
import com.dysi.AngryBoxClientManager.repositories.PaymentRepository;
import com.dysi.AngryBoxClientManager.services.PaymentService;

@Service
public class PaymentServiceImpl implements PaymentService {
	@Autowired
	private PaymentRepository paymentRepository;

	@Override
	public LicensePaymentsPaginationDTO getLicensePaymentsPaginated(License license, int pageSize, int pageNumber) {
		LicensePaymentsPaginationDTO lppDTO = new LicensePaymentsPaginationDTO();
		PageRequest request = new PageRequest(pageNumber - 1, pageSize);
		Page<Payment> paymentsPage = this.paymentRepository.findPaymentsPaginatedByLicense(license.getId(), request);
		
		lppDTO.setLicenseId(license.getId());
		lppDTO.setCompanyId(license.getCompanyId());
		lppDTO.setCompanyName(license.getCompanyName());
		lppDTO.setDescription(license.getDescription());
		lppDTO.setPayments(paymentsPage);
		
		return lppDTO;
	}

}
