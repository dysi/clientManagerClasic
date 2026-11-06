package com.dysi.AngryBoxClientManager.repositories;

import com.dysi.AngryBoxClientManager.models.BankAccount;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long>, JpaSpecificationExecutor<BankAccount> {
    @EntityGraph(attributePaths = "bank")
    Page<BankAccount> findAll(Specification<BankAccount> specification, Pageable pageable);

    @EntityGraph(attributePaths = "bank")
    java.util.List<BankAccount> findAll(Specification<BankAccount> specification, org.springframework.data.domain.Sort sort);

    Optional<BankAccount> findByCbu(String cbu);
    Optional<BankAccount> findByAlias(String alias);
}
