package org.springframework.data.cockroachdb.it.bank.repository;

import org.springframework.data.cockroachdb.it.TestProfiles;
import org.springframework.data.cockroachdb.it.bank.model.TransactionItem;

import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Profile(TestProfiles.JPA)
@Repository
public interface ActualJpaTransactionItemRepository extends JpaRepository<TransactionItem, TransactionItem.Id>,
        JpaSpecificationExecutor<TransactionItem> {
}
