package org.springframework.data.cockroachdb.it.bank.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.cockroachdb.it.TestProfiles;
import org.springframework.data.cockroachdb.it.bank.model.Transaction;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Profile(TestProfiles.JPA)
@Repository
public interface ActualJpaTransactionRepository extends JpaRepository<Transaction, UUID>,
        JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByToken(UUID token);
}
