package org.springframework.data.cockroachdb.it.bank.service;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.cockroachdb.it.bank.model.Account;
import org.springframework.data.cockroachdb.it.bank.model.Money;
import org.springframework.data.cockroachdb.it.bank.model.Transaction;
import org.springframework.data.cockroachdb.it.bank.model.TransferRequest;
import org.springframework.data.cockroachdb.it.bank.repository.AccountRepository;
import org.springframework.data.cockroachdb.it.bank.repository.TransactionRepository;
import org.springframework.data.cockroachdb.annotations.NotTransactional;
import org.springframework.data.cockroachdb.annotations.Retryable;
import org.springframework.data.cockroachdb.annotations.SetVariable;
import org.springframework.data.cockroachdb.annotations.TransactionBoundary;
import org.springframework.data.cockroachdb.annotations.Variable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class TransactionServiceImpl implements TransactionService {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    @TransactionBoundary(variables = {
            @SetVariable(variable = Variable.idle_in_transaction_session_timeout, value = "0"),
            @SetVariable(variable = Variable.enable_implicit_select_for_update, value = "on"),
    })
    @Retryable(retryAttempts = 50)
    public Transaction submitTransferRequest(TransferRequest request) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("No transaction context - check Spring profile settings");
        }

        Optional<Transaction> existingTransaction = transactionRepository.findTransactionByToken(request.getIdempotencyKey());
        if (existingTransaction.isPresent()) {
            return existingTransaction.get();
        }

        if (request.getAccountLegs().size() < 2) {
            throw new BadRequestException("Must have at least two legs");
        }

        // Coalesce multi-legged transactions
        final Map<Long, Pair<Money, String>> legs = coalesce(request);

        // Lookup accounts with authoritative reads
        final List<Account> accounts = accountRepository.findByIDs(legs.keySet());

        final Transaction.Builder transactionBuilder = Transaction.builder()
                .withToken(request.getIdempotencyKey())
                .withRegion(request.getRegion())
                .withTransactionType(request.getTransactionType())
                .withBookingDate(request.getBookingDate())
                .withTransferDate(request.getTransferDate());

        legs.forEach((accountId, value) -> {
            final Money amount = value.getFirst();

            Account account = accounts.stream().filter(a -> Objects.equals(a.getId(), accountId))
                    .findFirst().orElseThrow(() -> new NoSuchAccountException(accountId.toString()));

            transactionBuilder
                    .andItem()
                    .withRegion(request.getRegion())
                    .withAccount(account)
                    .withRunningBalance(account.getBalance())
                    .withAmount(amount)
                    .withNote(value.getSecond())
                    .then();

            account.addAmount(amount);
        });

        accountRepository.updateBalances(accounts);

        Transaction transaction = transactionBuilder.build();
        transactionRepository.createTransaction(transaction);

        return transaction;
    }

    private Map<Long, Pair<Money, String>> coalesce(TransferRequest request) {
        final Map<Long, Pair<Money, String>> legs = new HashMap<>();
        final Map<Currency, BigDecimal> amounts = new HashMap<>();

        // Compact accounts and verify that total balance for the legs with the same currency is zero
        request.getAccountLegs().forEach(leg -> {
            legs.compute(leg.getId(),
                    (key, amount) -> (amount == null)
                            ? Pair.of(leg.getAmount(), leg.getNote())
                            : Pair.of(amount.getFirst().plus(leg.getAmount()), leg.getNote()));
            amounts.compute(leg.getAmount().getCurrency(),
                    (currency, amount) -> (amount == null)
                            ? leg.getAmount().getAmount() : leg.getAmount().getAmount().add(amount));
        });

        // The sum of debits for all accounts must equal the corresponding sum of credits (per currency)
        amounts.forEach((key, value) -> {
            if (value.compareTo(BigDecimal.ZERO) != 0) {
                throw new BadRequestException("Unbalanced transaction: currency ["
                        + key + "], amount sum [" + value + "]");
            }
        });

        return legs;
    }

    @Override
    @NotTransactional
    public Page<Transaction> find(Pageable page) {
        return transactionRepository.findTransactions(page);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable
    public void deleteAll() {
        transactionRepository.deleteAll();
    }
}
