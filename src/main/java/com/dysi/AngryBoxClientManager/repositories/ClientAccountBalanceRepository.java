package com.dysi.AngryBoxClientManager.repositories;

import com.dysi.AngryBoxClientManager.models.ClientAccountBalance;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientAccountBalanceRepository extends JpaRepository<ClientAccountBalance, Long> {
    java.util.List<ClientAccountBalance> findByClientIdInAndRemoveDateIsNull(java.util.Collection<Long> clientIds);
    Optional<ClientAccountBalance> findByClientId(Long clientId);
    Optional<ClientAccountBalance> findByClientIdAndRemoveDateIsNull(Long clientId);
}
