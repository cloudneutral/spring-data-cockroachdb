package org.springframework.data.cockroachdb.basics;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cockroachdb.annotations.NotTransactional;
import org.springframework.data.cockroachdb.annotations.Retryable;
import org.springframework.data.cockroachdb.annotations.TimeTravel;
import org.springframework.data.cockroachdb.annotations.TransactionBoundary;
import org.springframework.data.cockroachdb.aspect.TimeTravelMode;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    @Autowired
    private AccountRepository accountRepository;

    @NotTransactional
    public Account create(Account account) {
        Account a = accountRepository.save(account);
        a.markNotNew();
        return a;
    }

    @NotTransactional
    public Account findByName(String name) {
        return accountRepository.findByName(name)
                .orElseThrow(() -> new NoSuchEntityException(Account.class, name));
    }

    @NotTransactional
    public Account findById(Long id) {
        return accountRepository.findById(id).orElseThrow(() -> new NoSuchEntityException(Account.class, id));
    }

    @TransactionBoundary
    @Retryable
    public Account update(Account account) {
        Account accountProxy = accountRepository.findById(account.getId())
                .orElseThrow(() -> new NoSuchEntityException(Account.class, account.getId()));
        accountProxy.setName(account.getName());
        accountProxy.setDescription(account.getDescription());
        accountProxy.setBalance(account.getBalance());
        accountProxy.setClosed(account.isClosed());
        return accountRepository.save(accountProxy);
    }

    @NotTransactional
    public BigDecimal getBalance(Long id) {
        return accountRepository.findBalanceById(id);
    }

    @TransactionBoundary(timeTravel = @TimeTravel(mode = TimeTravelMode.FOLLOWER_READ), readOnly = true)
    public BigDecimal getBalanceSnapshot_Explicit(Long id) {
        return accountRepository.findBalanceById(id);
    }

    @NotTransactional
    public BigDecimal getBalanceSnapshot_Implicit(Long id) {
        return accountRepository.findBalanceSnapshotById(id);
    }

    @TransactionBoundary
    public void delete(Long id) {
        accountRepository.deleteById(id);
    }

    @TransactionBoundary
    public void deleteAll() {
        accountRepository.deleteAll();
    }
}
