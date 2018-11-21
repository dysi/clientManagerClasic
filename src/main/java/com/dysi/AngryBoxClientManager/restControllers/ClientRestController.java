package com.dysi.AngryBoxClientManager.restControllers;

import com.dysi.AngryBoxClientManager.dtos.ClientDTO;
import com.dysi.AngryBoxClientManager.models.Client;
import com.dysi.AngryBoxClientManager.models.License;
import com.dysi.AngryBoxClientManager.services.ClientService;
import com.dysi.AngryBoxClientManager.services.LicenseService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ClientRestController {

    @Autowired
    private ClientService clientService;
    @Autowired
    private LicenseService licenseService;

    /**
     * COMUNICACION CON ANGRYBOX (Bloqueo de Cuenta)
     * @param username
     * @param companyId
     * @return
     */
    @RequestMapping("/clients/license")
    public ResponseEntity<License> getLicenseFromClient(
            @RequestParam(value = "username", required = true) String username,
            @RequestParam(value = "companyId", required = true) Long companyId
    ) {
        try {
            Client client = this.clientService.getClientFromUser(username);
            License license = this.licenseService.getLicenseFromClient(client, companyId);
            if (license == null) {
                return new ResponseEntity<License>(license, HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<License>(license, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<License>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @RequestMapping("/clients/getClients")
    public ResponseEntity<List<ClientDTO>> getClients() {
        try {
            List<ClientDTO> client = this.clientService.getClients();
            if (client == null) {
                return new ResponseEntity<List<ClientDTO>>(client, HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<List<ClientDTO>>(client, HttpStatus.OK);
        } catch (Exception e) {
        	return new ResponseEntity<List<ClientDTO>>(HttpStatus.BAD_REQUEST);
        }
    }
    
    @RequestMapping(value = "/clients/edit", method = RequestMethod.POST)
    public ResponseEntity<String> editClient(
    		@RequestBody ClientDTO client) {
        try {
        	
        	String resultado = this.clientService.editClient(client);

            return new ResponseEntity<String>(resultado, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
        }
    }

}


