package com.dysi.AngryBoxClientManager.servicesImpl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.dysi.AngryBoxClientManager.dtos.LicensePaymentsPaginationDTO;
import com.dysi.AngryBoxClientManager.dtos.PaymentDTO;
import com.dysi.AngryBoxClientManager.models.License;
import com.dysi.AngryBoxClientManager.models.Payment;
import com.dysi.AngryBoxClientManager.repositories.PaymentRepository;
import com.dysi.AngryBoxClientManager.services.PaymentService;

@Service
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {
	@Autowired
	private PaymentRepository paymentRepository;

	@Override
	public LicensePaymentsPaginationDTO getLicensePaymentsPaginated(License license, int pageSize, int pageNumber) {
		
		LicensePaymentsPaginationDTO lppDTO = new LicensePaymentsPaginationDTO();
		PageRequest request = PageRequest.of(pageNumber - 1, pageSize);
		Page<Payment> paymentsPage = this.paymentRepository.findPaymentsPaginatedByLicense(license.getId(), request);
		
		lppDTO.setLicenseId(license.getId());
		lppDTO.setCompanyId(license.getCompanyId());
		lppDTO.setCompanyName(license.getCompanyName());
		lppDTO.setDescription(license.getDescription());
		lppDTO.setPayments(paymentsPage.map(WebAdminService::paymentView));
		
		return lppDTO;
	}
	
	@Override
	public List<com.dysi.AngryBoxClientManager.dtos.AdminViews.PaymentView> getLicensePayments(long licenseId) {

		List<Payment> paymentsPage = this.paymentRepository.findPaymentsPaginatedByLicense(licenseId);
		
		return paymentsPage.stream().map(WebAdminService::paymentView).collect(java.util.stream.Collectors.toList());
	}

    @Override
    public String editPayment(PaymentDTO input) {
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT, "El contrato anterior de pagos fue retirado.");
    }

    @Override
    public String pagoBaja(Long id) {
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT, "Los pagos no admiten anulación ni reactivación.");
    }
}
