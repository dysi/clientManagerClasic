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
            @RequestParam(value = "pageSize", required = true) int pageSize,
            @RequestParam(value = "pageNumber", required = true) int pageNumber    
    ) {
        try {
            Client client = this.clientService.getClientFromUser(username);
            License license = this.licenseService.getLicenseFromClient(client, companyId);
            LicensePaymentsPaginationDTO lppDTO = this.paymentService.getLicensePaymentsPaginated(license, pageSize, pageNumber);
            
            if (license == null) {
                return new ResponseEntity<LicensePaymentsPaginationDTO>(lppDTO, HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<LicensePaymentsPaginationDTO>(lppDTO, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<LicensePaymentsPaginationDTO>(HttpStatus.BAD_REQUEST);
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
    
    @RequestMapping("/payments/paymentsByCompany")
    public ResponseEntity<List<Payment>> getPaymentsPaginatedByCompany(
            @RequestParam(value = "companyId", required = true) Long companyId,
            @RequestParam(value = "clientID", required = true) Long clientID) {
        try {
            
        	List<Payment> payments = this.paymentService.getLicensePayments(clientID, companyId);

            return new ResponseEntity<List<Payment>>(payments, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<List<Payment>>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @RequestMapping(value = "/payments/edit", method = RequestMethod.POST)
    public ResponseEntity<String> editLicencia(
    		@RequestBody LicenseDTO license) {
        try {
        	
        	String resultado = this.licenseService.editLicencia(license);

            return new ResponseEntity<String>(resultado, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
        }
    }
}
