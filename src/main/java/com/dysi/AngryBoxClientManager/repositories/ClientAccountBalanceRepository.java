package com.dysi.AngryBoxClientManager.repositories;

import com.dysi.AngryBoxClientManager.models.ClientAccountBalance;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientAccountBalanceRepository extends JpaRepository<ClientAccountBalance, Long> {
    Optional<ClientAccountBalance> findByClientIdAndRemoveDateIsNull(Long clientId);
}
