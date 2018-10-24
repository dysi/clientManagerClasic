package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.dtos.LicenseDTO;
import com.dysi.AngryBoxClientManager.models.Client;
import com.dysi.AngryBoxClientManager.models.License;
import com.dysi.AngryBoxClientManager.models.LicenseStatus;
import com.dysi.AngryBoxClientManager.models.Payment;
import com.dysi.AngryBoxClientManager.repositories.ClientRepository;
import com.dysi.AngryBoxClientManager.repositories.LicenseRepository;
import com.dysi.AngryBoxClientManager.services.LicenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Service
public class LicenseServiceImpl implements LicenseService {

    @Autowired
    private LicenseRepository licenseRepository;
    @Autowired
    private ClientRepository clientRepository;

    @Override
    public License getLicenseFromClient(Client client, long companyId) {
        List<License> licenseList = this.licenseRepository.findLicensesByClient(client);
        License license = null;

        for(License l : licenseList){
            if(l.getCompanyId() == companyId){
            	license = l;
                break;
            }
        }

        return license;
    }
    
    @Override
    public List<License> getLicensesFromClient(Client client) {
        List<License> licenseList = this.licenseRepository.findLicensesByClient(client);

        return licenseList;
    }

	@Override
	public String editLicencia(LicenseDTO licDTO) {
		License licenseEdit = new License();
		boolean edicion = (licDTO.getId() != null && licDTO.getId() > 0);
		
		if(edicion){
			//EDICION Y ID CLIENTE SE MANTIENE
			licenseEdit = this.licenseRepository.findOne(licDTO.getId());
			LicenseStatus licenciaBase = licenseEdit.getLicenseStatuses().get(0);
			if(licDTO.getLockDate() != null){
				licenciaBase.setLockDate(licDTO.getLockDate());
			} else {
				Calendar lockDate = Calendar.getInstance();
				// number of days to add
				lockDate.add(Calendar.DATE, 90);
				licenciaBase.setLockDate(lockDate);
			}
		} else {
			//NUEVO
			List<License> companyExiste = this.licenseRepository.findLicensesByCompanyId(licDTO.getCompanyId());
			if(companyExiste != null && companyExiste.size() > 0){
				return ("99|EL ID DE LA COMPAÑIA YA ESTA EN USO: #" + companyExiste.get(0).getCompanyId() + ". " + companyExiste.get(0).getCompanyName());
			}
			Client client = clientRepository.findOne(licDTO.getClienteId());
			if(client == null){
				return ("98|NO SE ENCONTRO EL CLIENTE: #" + licDTO.getClienteId());
			}
			licenseEdit.setClient(client);
		}
		
		licenseEdit.setCompanyId(licDTO.getCompanyId());
		licenseEdit.setCompanyName(licDTO.getCompanyName());
		licenseEdit.setDescription(licDTO.getDescription());
		
//		Calendar initialActivity = Calendar.getInstance();
//		initialActivity.setTimeInMillis(licDTO.getInitialActivity().getTime());
//      licenseEdit.setInitialActivity(initialActivity);
        
		licenseEdit.setInitialActivity(licDTO.getInitialActivity());
        
		licenseEdit.setPrePayment(licDTO.isPrePayment());
		
		licenseEdit = this.licenseRepository.saveAndFlush(licenseEdit);
		
		if(!edicion){
			List<Payment> paymentList = new ArrayList<Payment>();
			licenseEdit.setPaymentList(paymentList);
			
			List<LicenseStatus> licenseStatuses = new ArrayList<LicenseStatus>();
			
			LicenseStatus licenciaBase = new LicenseStatus();
			
			licenciaBase.setLicense(licenseEdit);
			
			licenciaBase.setCreationDate(Calendar.getInstance());
			licenciaBase.setForcedLock(false);
			licenciaBase.setLimitTransaction(9999999l);
			
			licenciaBase.setObservation("");
			if(licDTO.getLockDate() != null){
				licenciaBase.setLockDate(licDTO.getLockDate());
			} else {
				Calendar lockDate = Calendar.getInstance();
				// number of days to add
				lockDate.add(Calendar.DATE, 90);
				licenciaBase.setLockDate(lockDate);
			}
			
			licenseStatuses.add(licenciaBase);
			
			licenseEdit.setLicenseStatuses(licenseStatuses);
			
			this.licenseRepository.save(licenseEdit);
		} 
		
		return "1|Licencia " + (edicion ? "Editada" : "Creada") + " Exitosamente";
	}

	@Override
	public String licenciaBloquear(Long licenciaID) {
		License licenseEdit = this.licenseRepository.findOne(licenciaID);
		if(licenseEdit == null){
			return ("99|EL ID DE LA LICENCIA NO EXISTE: #" + licenciaID);
		}
		
		List<LicenseStatus> estados = licenseEdit.getLicenseStatuses();
		estados.get(0).setForcedLock(!estados.get(0).isForcedLock());
		
		this.licenseRepository.save(licenseEdit);
		
		return "1|Licencia #" + licenciaID + " Cambio de Estado";
	}
}
