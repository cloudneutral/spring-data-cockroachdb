package org.springframework.data.cockroachdb.it.bank.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;

import org.springframework.data.cockroachdb.annotations.TransactionBoundary;
import org.springframework.data.cockroachdb.it.TestProfiles;
import org.springframework.data.cockroachdb.it.bank.model.Account;
import org.springframework.data.cockroachdb.it.bank.model.AccountSummary;
import org.springframework.data.cockroachdb.annotations.NotTransactional;
import org.springframework.data.cockroachdb.it.bank.model.Money;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Profile(TestProfiles.JPA)
@Repository
public class JpaAccountRepositoryImpl implements AccountRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ActualJpaAccountRepository accountRepository;

    @Override
    @NotTransactional
    public Integer nextSequenceNumber() {
        return accountRepository.nextSequenceNumber();
    }

    @Override
    @NotTransactional
    public Money getBalance(Long id) {
        return accountRepository.findBalanceById(id);
    }

    @Override
    @NotTransactional
    public Money getBalanceSnapshot(Long id) {
        return accountRepository.findBalanceSnapshotById(id);
    }

    @Override
    @TransactionBoundary
    public void createAccounts(int numAccounts, int batchSize, Supplier<Account> accountSupplier) {
        final List<Integer> batchSequence = new ArrayList<>();

        final Supplier<Integer> sequenceIds = () -> {
            if (batchSequence.isEmpty()) {
                int nextNo = nextSequenceNumber();
                IntStream.rangeClosed(nextNo, nextNo - 1 + 64).forEach(batchSequence::add);
            }
            return batchSequence.remove(0);
        };

        List<Account> batch = new ArrayList<>();

        IntStream.rangeClosed(1, numAccounts).forEach(value -> {
            Account account = accountSupplier.get();
            account.setName("user:" + sequenceIds.get());
            batch.add(account);
        });

        Session session = entityManager.unwrap(Session.class);
        session.setJdbcBatchSize(batchSize);

        accountRepository.saveAll(batch);
    }

    @Override
    public void updateBalances(List<Account> accounts) {
        // No-op, expect batch updates via transparent persistence
    }

    @Override
    public List<Account> findByIDs(Set<Long> ids) {
        return accountRepository.findAllForUpdate(new HashSet<>(ids));
    }

    @Override
    public List<Account> findByRegion(String region, int offset, int limit) {
        return entityManager.createQuery("SELECT a FROM Account a WHERE a.region=?1",
                        Account.class)
                .setParameter(1, region)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public List<Money> getTotalBalance() {
        return entityManager.createQuery(
                        "SELECT new org.springframework.data.cockroachdb.it.bank.model.Money(sum(a.balance.amount),a.balance.currency) "
                                + "FROM Account a group by a.balance.currency", Money.class)
                .getResultList();
    }

    @Override
    public AccountSummary reportSummary(String region) {
        List<AccountSummary> result = new LinkedList<>();

        try (Stream<Tuple> stream = accountRepository.accountSummary(region)) {
            stream.forEach(o -> {
                AccountSummary summary = new AccountSummary();
                summary.setRegion(region);
                summary.setNumberOfAccounts((o.get(0, Long.class).intValue()));
                summary.setTotalBalance(o.get(1, BigDecimal.class));
                summary.setMinBalance(o.get(2, BigDecimal.class));
                summary.setMaxBalance(o.get(3, BigDecimal.class));
                summary.setAvgBalance(new BigDecimal(o.get(4, Double.class)));
                summary.setCurrency(o.get(5, Currency.class));

                result.add(summary);
            });
        }

        return result.iterator().next();
    }

    @Override
    public void deleteAll() {
        accountRepository.deleteAllInBatch();
    }
}
