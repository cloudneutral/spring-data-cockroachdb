package org.springframework.data.cockroachdb.it.bank.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.cockroachdb.it.TestProfiles;
import org.springframework.data.cockroachdb.it.bank.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Profile(TestProfiles.JPA)
@Repository
public class JpaTransactionRepositoryImpl implements TransactionRepository {
    @Autowired
    private ActualJpaTransactionRepository transactionRepository;

    @Autowired
    private ActualJpaTransactionItemRepository itemRepository;

    @Override
    public Optional<Transaction> findTransactionByToken(UUID token) {
        return transactionRepository.findByToken(token);
    }

    @Override
    public Transaction createTransaction(Transaction transaction) {
        Transaction attached = transactionRepository.save(transaction);
        transaction.getItems().forEach(transactionItem
                -> transactionItem.setTransaction(attached));
        itemRepository.saveAll(transaction.getItems());
        return attached;
    }

    @Override
    public Page<Transaction> findTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }

    @Override
    public void deleteAll() {
        itemRepository.deleteAllInBatch();
        transactionRepository.deleteAllInBatch();
    }
}
