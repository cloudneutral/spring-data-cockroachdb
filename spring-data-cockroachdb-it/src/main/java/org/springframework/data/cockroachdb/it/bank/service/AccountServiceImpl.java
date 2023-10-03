package org.springframework.data.cockroachdb.it.bank.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cockroachdb.annotations.NotTransactional;
import org.springframework.data.cockroachdb.annotations.TransactionBoundary;
import org.springframework.data.cockroachdb.it.bank.model.Account;
import org.springframework.data.cockroachdb.it.bank.model.AccountType;
import org.springframework.data.cockroachdb.it.bank.model.ForeignSystem;
import org.springframework.data.cockroachdb.it.bank.model.Money;
import org.springframework.data.cockroachdb.it.bank.repository.AccountRepository;
import org.springframework.data.cockroachdb.it.util.RandomUtils;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl implements AccountService {
    @Autowired
    private AccountRepository accountRepository;

    @Override
    @NotTransactional
    public void createAccounts(String region,
                               Money initialBalance,
                               int numAccounts,
                               int batchSize,
                               Consumer<Integer> progress) {
        AtomicInteger counter = new AtomicInteger();
        Supplier<Account> accountSupplier = () -> {
            progress.accept(counter.incrementAndGet());
            return Account.builder()
                    .withRegion(region)
                    .withBalance(initialBalance)
                    .withAccountType(AccountType.ASSET)
                    .withAllowNegative(false)
                    .withClosed(false)
                    .withForeignSystem(ForeignSystem.builder()
                            .withId(RandomUtils.random.nextInt(1, 1000) + "")
                            .withLabel("System X")
                            .withOwner("Chuck Norris")
                            .withInceptionTime(LocalDateTime.now())
                            .build())
                    .build();
        };

        accountRepository.createAccounts(numAccounts, batchSize, accountSupplier);
    }

    @Override
    @NotTransactional
    public List<Account> findAccountsByRegion(String region, int offset, int limit) {
        return accountRepository.findByRegion(region, offset, limit);
    }

    @Override
    @NotTransactional
    public Money getBalance(Long id) {
        return accountRepository.getBalance(id);
    }

    @Override
    @NotTransactional
    public Money getBalanceSnapshot(Long id) {
        return accountRepository.getBalanceSnapshot(id);
    }


    @Override
    @NotTransactional
    public List<Money> getTotalBalance() {
        return accountRepository.getTotalBalance();
    }

    @Override
    @TransactionBoundary
    public void deleteAll() {
        accountRepository.deleteAll();
    }
}
