package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.models.Client;
import com.dysi.AngryBoxClientManager.models.License;
import com.dysi.AngryBoxClientManager.repositories.LicenseRepository;
import com.dysi.AngryBoxClientManager.services.LicenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LicenseServiceImpl implements LicenseService {

    @Autowired
    private LicenseRepository licenseRepository;

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
}
