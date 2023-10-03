package org.springframework.data.cockroachdb.it.bank.service;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.data.cockroachdb.it.bank.model.Account;
import org.springframework.data.cockroachdb.it.bank.model.Money;

public interface AccountService {
    void createAccounts(String region,
                        Money initialBalance,
                        int numAccounts,
                        int batchSize,
                        Consumer<Integer> progress);

    List<Account> findAccountsByRegion(String region, int offset, int limit);

    Money getBalance(Long id);

    Money getBalanceSnapshot(Long id);

    List<Money> getTotalBalance();

    void deleteAll();
}
