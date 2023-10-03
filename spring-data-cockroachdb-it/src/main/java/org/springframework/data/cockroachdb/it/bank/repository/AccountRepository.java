package org.springframework.data.cockroachdb.it.bank.repository;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.data.cockroachdb.it.bank.model.Account;
import org.springframework.data.cockroachdb.it.bank.model.AccountSummary;
import org.springframework.data.cockroachdb.it.bank.model.Money;

public interface AccountRepository {
    Integer nextSequenceNumber();

    void createAccounts(int numAccounts, int batchSize, Supplier<Account> accountSupplier);

    Money getBalance(Long id);

    Money getBalanceSnapshot(Long id);

    List<Money> getTotalBalance();

    List<Account> findByRegion(String region, int offset, int limit);

    List<Account> findByIDs(Set<Long> ids);

    void updateBalances(List<Account> accounts);

    AccountSummary reportSummary(String region);

    void deleteAll();
}
