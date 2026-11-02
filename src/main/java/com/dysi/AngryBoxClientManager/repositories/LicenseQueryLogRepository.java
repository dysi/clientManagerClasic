package com.dysi.AngryBoxClientManager.repositories;

import com.dysi.AngryBoxClientManager.models.LicenseQueryLog;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

public interface LicenseQueryLogRepository extends JpaRepository<LicenseQueryLog, Long>, JpaSpecificationExecutor<LicenseQueryLog> {
    @Query("select q.id from LicenseQueryLog q where q.receivedAt < :cutoff order by q.receivedAt, q.id")
    List<Long> expired(@org.springframework.data.repository.query.Param("cutoff") Instant cutoff, Pageable page);
}
