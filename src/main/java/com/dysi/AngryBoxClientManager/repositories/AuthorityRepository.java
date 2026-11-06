package com.dysi.AngryBoxClientManager.repositories;

import com.dysi.AngryBoxClientManager.models.Authority;
import java.util.List;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorityRepository extends JpaRepository<Authority, Long>{
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Authority a order by a.id")
    List<Authority> findAllForAssignment();
}
