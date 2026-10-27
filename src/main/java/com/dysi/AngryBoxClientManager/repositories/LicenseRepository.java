package com.dysi.AngryBoxClientManager.repositories;

import com.dysi.AngryBoxClientManager.models.Client;
import com.dysi.AngryBoxClientManager.models.License;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LicenseRepository extends JpaRepository<License, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<License> {

    @org.springframework.data.jpa.repository.Query("select new com.dysi.AngryBoxClientManager.dtos.PortalViews$LicenseTotals("
            + "count(l), sum(case when s.forcedLock = true then 1 else 0 end), "
            + "sum(case when s.forcedLock = false and s.lockDate < :tomorrow then 1 else 0 end), "
            + "sum(case when s.forcedLock = false and s.lockDate >= :tomorrow and s.lockDate < :day16 then 1 else 0 end), "
            + "sum(case when s.id is null or (s.forcedLock = false and s.lockDate is null) then 1 else 0 end)) "
            + "from License l left join l.licenseStatuses s on s.removeDate is null "
            + "where l.client.id = :clientId and l.removeDate is null "
            + "and (s.id is null or s.id = (select min(candidate.id) from LicenseStatus candidate "
            + "where candidate.license = l and candidate.removeDate is null))")
    com.dysi.AngryBoxClientManager.dtos.PortalViews.LicenseTotals portalTotals(
            @org.springframework.data.repository.query.Param("clientId") Long clientId,
            @org.springframework.data.repository.query.Param("tomorrow") java.util.Calendar tomorrow,
            @org.springframework.data.repository.query.Param("day16") java.util.Calendar day16);

    public List<License> findLicensesByClient(Client client);
    
    public List<License> findLicensesByCompanyId(Long companyId);

}
