package com.dysi.AngryBoxClientManager.repositories;

import com.dysi.AngryBoxClientManager.models.AngryboxSyncState;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AngryboxSyncStateRepository extends JpaRepository<AngryboxSyncState, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AngryboxSyncState s where s.companyId = :companyId")
    Optional<AngryboxSyncState> lockSource(@Param("companyId") Long companyId);
}
