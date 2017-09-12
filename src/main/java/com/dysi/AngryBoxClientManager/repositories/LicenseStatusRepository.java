package com.dysi.AngryBoxClientManager.repositories;

import com.dysi.AngryBoxClientManager.models.LicenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LicenseStatusRepository extends JpaRepository<LicenseStatus, Long> {
}
