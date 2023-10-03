package org.springframework.data.cockroachdb.it.bank.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.cockroachdb.it.bank.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionRepository {
    Optional<Transaction> findTransactionByToken(UUID token);

    Transaction createTransaction(Transaction transaction);

    Page<Transaction> findTransactions(Pageable pageable);

    void deleteAll();
}
