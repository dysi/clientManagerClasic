package com.dysi.AngryBoxClientManager.repositories;

import com.dysi.AngryBoxClientManager.models.AngryboxSyncBatch;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AngryboxSyncBatchRepository extends JpaRepository<AngryboxSyncBatch, Long> {
    Optional<AngryboxSyncBatch> findByCompanyIdAndSyncUuid(Long companyId, String syncUuid);
}
