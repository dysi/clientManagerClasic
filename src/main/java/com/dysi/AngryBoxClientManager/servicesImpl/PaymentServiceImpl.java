package com.dysi.AngryBoxClientManager.servicesImpl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.dysi.AngryBoxClientManager.dtos.LicensePaymentsPaginationDTO;
import com.dysi.AngryBoxClientManager.dtos.PaymentDTO;
import com.dysi.AngryBoxClientManager.models.License;
import com.dysi.AngryBoxClientManager.models.Payment;
import com.dysi.AngryBoxClientManager.repositories.LicenseRepository;
import com.dysi.AngryBoxClientManager.repositories.PaymentRepository;
import com.dysi.AngryBoxClientManager.services.PaymentService;

@Service
public class PaymentServiceImpl implements PaymentService {
	@Autowired
	private PaymentRepository paymentRepository;
	@Autowired
    private LicenseRepository licenseRepository;

	@Override
	public LicensePaymentsPaginationDTO getLicensePaymentsPaginated(License license, int pageSize, int pageNumber) {
		
		LicensePaymentsPaginationDTO lppDTO = new LicensePaymentsPaginationDTO();
		PageRequest request = PageRequest.of(pageNumber - 1, pageSize);
		Page<Payment> paymentsPage = this.paymentRepository.findPaymentsPaginatedByLicense(license.getId(), request);
		
		lppDTO.setLicenseId(license.getId());
		lppDTO.setCompanyId(license.getCompanyId());
		lppDTO.setCompanyName(license.getCompanyName());
		lppDTO.setDescription(license.getDescription());
		lppDTO.setPayments(paymentsPage);
		
		return lppDTO;
	}
	
	@Override
	public List<Payment> getLicensePayments(long licenseId) {

		List<Payment> paymentsPage = this.paymentRepository.findPaymentsPaginatedByLicense(licenseId);
		
		return (paymentsPage != null ? paymentsPage : new ArrayList<Payment>());
	}

	@Override
	public String editPayment(PaymentDTO payDTO) {
		
		License licenseEdit = this.licenseRepository.findById(payDTO.getLicenseId()).orElse(null);
		if(licenseEdit == null){
			return ("99|EL ID DE LA LICENCIA NO EXISTE: #" + payDTO.getLicenseId());
		}
		
		List<Payment> payments = (licenseEdit.getPaymentList() != null ? licenseEdit.getPaymentList() : new ArrayList<Payment>());
		
		Payment paymentEdit = new Payment();
		boolean edicion = (payDTO.getId() != null && payDTO.getId() > 0);
		if(edicion){
			//EDICION Y ID CLIENTE SE MANTIENE
			for (Payment payment : payments) {
				if(payment.getId().longValue() == payDTO.getId()){
					paymentEdit = payment;
					break;
				}
			}
		} else {
			//NUEVO
			paymentEdit.setCreationDate(Calendar.getInstance());
			paymentEdit.setLicense(licenseEdit);
			payments.add(paymentEdit);
		}
				
		paymentEdit.setAmount(payDTO.getAmount() != null ? payDTO.getAmount() : 0d);
		paymentEdit.setDiscount(payDTO.getDiscount() != null ? payDTO.getDiscount() : 0d);
		paymentEdit.setExpirationDate(payDTO.getExpirationDate() != null ? payDTO.getExpirationDate() : null);
		paymentEdit.setObservation(payDTO.getObservation() != null ? payDTO.getObservation() : "");
		paymentEdit.setPaymentDate(payDTO.getPaymentDate() != null ? payDTO.getPaymentDate() : null);
		paymentEdit.setPaymentRealDate(payDTO.getPaymentRealDate() != null ? payDTO.getPaymentRealDate() : null);
		
		
		licenseEdit = this.licenseRepository.saveAndFlush(licenseEdit);
		
		return "1|Pago " + (edicion ? "Editado" : "Creado") + " Exitosamente";
	}

	@Override
	public String pagoBaja(Long pagoID) {
		Payment paymentEdit = this.paymentRepository.findById(pagoID).orElse(null);
		if(paymentEdit == null){
			return ("99|EL ID DEL PAGO NO EXISTE: #" + pagoID);
		}
		
		paymentEdit.setRemoveDate(paymentEdit.getRemoveDate() != null ? null : Calendar.getInstance());
		
		this.paymentRepository.save(paymentEdit);
		
		return "1|PAGO #" + pagoID + " Cambio de Estado";
	}

}
