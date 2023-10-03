package org.springframework.data.cockroachdb.it.bank.service;

import org.springframework.data.cockroachdb.it.bank.model.Transaction;
import org.springframework.data.cockroachdb.it.bank.model.TransferRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionService {
    Transaction submitTransferRequest(TransferRequest transferRequest);

    Page<Transaction> find(Pageable page);

    void deleteAll();
}
