package com.dysi.AngryBoxClientManager.services;

import com.dysi.AngryBoxClientManager.models.Client;
import com.dysi.AngryBoxClientManager.models.License;

public interface LicenseService {
    public License getLicenseFromClient(Client client, long companyId);

}
