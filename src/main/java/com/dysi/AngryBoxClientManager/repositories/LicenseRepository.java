package com.dysi.AngryBoxClientManager.repositories;

import com.dysi.AngryBoxClientManager.models.Client;
import com.dysi.AngryBoxClientManager.models.License;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LicenseRepository extends JpaRepository<License, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<License> {

    public List<License> findLicensesByClient(Client client);
    
    public List<License> findLicensesByCompanyId(Long companyId);

}
