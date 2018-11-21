package com.dysi.AngryBoxClientManager.restControllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dysi.AngryBoxClientManager.dtos.LicenseDTO;
import com.dysi.AngryBoxClientManager.dtos.LicensePaymentsPaginationDTO;
import com.dysi.AngryBoxClientManager.dtos.PaymentDTO;
import com.dysi.AngryBoxClientManager.models.Client;
import com.dysi.AngryBoxClientManager.models.License;
import com.dysi.AngryBoxClientManager.models.Payment;
import com.dysi.AngryBoxClientManager.services.ClientService;
import com.dysi.AngryBoxClientManager.services.LicenseService;
import com.dysi.AngryBoxClientManager.services.PaymentService;

@RestController
@RequestMapping("/api")
public class PaymentRestController {
    @Autowired
    private ClientService clientService;
    @Autowired
    private LicenseService licenseService;
    @Autowired
    private PaymentService paymentService;
    
    @RequestMapping("/payments/paginated")
    public ResponseEntity<LicensePaymentsPaginationDTO> getPaymentsPaginated(
            @RequestParam(value = "username", required = true) String username,
            @RequestParam(value = "companyId", required = true) Long companyId,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "pageNumber", required = false) Integer pageNumber    
    		) {
        try {
        	pageSize = (pageSize == null ? 30 : pageSize);
        	pageNumber = (pageNumber == null ? 1 : pageNumber);
        	
            Client client = this.clientService.getClientFromUser(username);
            License license = this.licenseService.getLicenseFromClient(client, companyId);
            LicensePaymentsPaginationDTO lppDTO = this.paymentService.getLicensePaymentsPaginated(license, pageSize, pageNumber);
            
            if (license == null) {
                return new ResponseEntity<LicensePaymentsPaginationDTO>(lppDTO, HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<LicensePaymentsPaginationDTO>(lppDTO, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<LicensePaymentsPaginationDTO>(HttpStatus.FORBIDDEN);
        }
    }
    
    @RequestMapping("/payments/licenses")
    public ResponseEntity<List<License>> getLicenses(
            @RequestParam(value = "clientID", required = true) Long clientID) {
        try {
            Client client = this.clientService.getClientByID(clientID);
            List<License> licenses = this.licenseService.getLicensesFromClient(client);

            return new ResponseEntity<List<License>>(licenses, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<List<License>>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @RequestMapping("/payments/licenciaBloquear")
    public ResponseEntity<String> licenciaBloquear(
    		@RequestParam(value = "licenciaID", required = true) Long licenciaID) {
    	String resultado = "";
        try {
        	resultado = this.licenseService.licenciaBloquear(licenciaID);

            return new ResponseEntity<String>(resultado, HttpStatus.OK);
        } catch (Exception e) {
        	return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @RequestMapping("/payments/paymentsByLicense")
    public ResponseEntity<List<Payment>> getPaymentsPaginatedByCompany(
            @RequestParam(value = "licenseId", required = true) Long licenseId) {
        try {
            
        	List<Payment> payments = this.paymentService.getLicensePayments(licenseId);

            return new ResponseEntity<List<Payment>>(payments, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<List<Payment>>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @RequestMapping(value = "/payments/paymentEdit", method = RequestMethod.POST)
    public ResponseEntity<String> paymentEdit(
    		@RequestBody LicenseDTO license) {
        try {
        	
        	String resultado = this.licenseService.editLicencia(license);

            return new ResponseEntity<String>(resultado, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @RequestMapping(value = "/payments/licenseEdit", method = RequestMethod.POST)
    public ResponseEntity<String> licenseEdit(
    		@RequestBody PaymentDTO Payment) {
        try {
        	
        	String resultado = this.paymentService.editPayment(Payment);

            return new ResponseEntity<String>(resultado, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @RequestMapping("/payments/bajaPago")
    public ResponseEntity<String> bajaPago(
    		@RequestParam(value = "pagoID", required = true) Long pagoID) {
    	String resultado = "";
        try {
        	resultado = this.paymentService.pagoBaja(pagoID);

            return new ResponseEntity<String>(resultado, HttpStatus.OK);
        } catch (Exception e) {
        	return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
        }
    }
}
