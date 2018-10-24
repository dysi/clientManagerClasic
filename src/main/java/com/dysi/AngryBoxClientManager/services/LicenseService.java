package com.dysi.AngryBoxClientManager.services;

import java.util.List;

import com.dysi.AngryBoxClientManager.dtos.LicenseDTO;
import com.dysi.AngryBoxClientManager.models.Client;
import com.dysi.AngryBoxClientManager.models.License;

public interface LicenseService {
    public License getLicenseFromClient(Client client, long companyId);
    
    public List<License> getLicensesFromClient(Client client);

	public String editLicencia(LicenseDTO license);

	public String licenciaBloquear(Long licenciaID);

}
