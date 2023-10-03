package org.springframework.data.cockroachdb.it.bank;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.data.cockroachdb.it.bank.model.Account;
import org.springframework.data.cockroachdb.it.bank.model.Money;
import org.springframework.data.cockroachdb.it.bank.model.Transaction;
import org.springframework.data.cockroachdb.it.bank.model.TransferRequest;
import org.springframework.data.cockroachdb.it.util.BoundedThreadPool;
import org.springframework.data.cockroachdb.it.util.RandomUtils;
import org.springframework.data.cockroachdb.it.util.TextUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

public class ConcurrentBankTest extends AbstractBankIntegrationTest {
    private final BoundedThreadPool boundedThreadPool = new BoundedThreadPool(
            Runtime.getRuntime().availableProcessors() * 30); // Mostly I/O waits, so set aggressive thread count

    final int numAccounts = 50_000;

    final int accountLimit = 25; // Low number to increase contention

    final int numTasks = 1000;

    final Money initialBalance = Money.of("50000.00", Money.EUR);

    @Test
    @Order(1)
    public void whenStartingTest_setupTestFixture() {
        logger.info("Deleting previous transaction history..");
        transactionService.deleteAll();

        logger.info("Deleting previous account plan..");
        accountService.deleteAll();

        logger.info("Creating new account plan..");
        accountService.createAccounts("eu", initialBalance, numAccounts, 128, n -> {
            System.out.printf("\r%s", TextUtils.progressBar(numAccounts, n));
            if (n % 128 == 0) {
                System.out.println();
            }
        });
        logPoolStats();
    }

    @Test
    @Order(2)
    public void whenTransferFundsConcurrently_expectBalancedOutcome() {
        MetricsRetryListener.reset();

        Money totalBefore =
                accountService.getTotalBalance().stream()
                        .filter(money -> money.getCurrency().equals(Money.EUR))
                        .findFirst().orElseThrow(() -> new IllegalStateException("No total found?"));

        List<Account> accounts = accountService.findAccountsByRegion("eu", 0, accountLimit);

        Assertions.assertEquals(accountLimit, accounts.size());

        List<Future<Transaction>> futureTransactions = new ArrayList<>();

        logger.info("Queuing {} tasks..", numTasks);

        IntStream.rangeClosed(1, numTasks).forEach(value -> {
            Future<Transaction> future = boundedThreadPool.submit(() -> {
                UUID idempotencyKey = UUID.randomUUID();

                TransferRequest.Builder builder = TransferRequest.builder()
                        .withId(idempotencyKey)
                        .withBookingDate(LocalDate.now())
                        .withTransferDate(LocalDate.now())
                        .withRegion("eu")
                        .withTransactionType("gen");

                Money amount = RandomUtils.randomMoneyBetween(1.00, 150.00, Money.EUR);

                Set<Account> consumed = new HashSet<>();

                builder.withRegion("eu")
                        .addLeg()
                        .withId(RandomUtils.selectRandomUnique(accounts, consumed).getId())
                        .withAmount(amount)
                        .withNote("Credit note")
                        .then()
                        .addLeg()
                        .withId(RandomUtils.selectRandomUnique(accounts, consumed).getId())
                        .withAmount(amount.negate())
                        .withNote("Debit note")
                        .then();

                return transactionService.submitTransferRequest(builder.build());
            });
            futureTransactions.add(future);
        });

        logPoolStats();

        logger.info("All {} tasks queued - awaiting completion", futureTransactions.size());

        int successCount = 0, failureCount = 0;

        List<Throwable> executionErrors = new ArrayList<>();

        while (!futureTransactions.isEmpty()) {
            try {
                Transaction transaction = futureTransactions.remove(0).get();
                logger.info("Transaction [ID={}] completed - {} workers remaining [{}]",
                        transaction.getId(),
                        futureTransactions.size(),
                        boundedThreadPool);
                Assertions.assertNotNull(transaction.getId());
                Assertions.assertEquals(2, transaction.getItems().size());
                successCount++;
            } catch (InterruptedException e) {
                Assertions.fail(e);
                break;
            } catch (ExecutionException e) {
                executionErrors.add(e.getCause());
                failureCount++;
            }
        }

        boundedThreadPool.shutdownAndDrain();

        logger.info(formatSuccessRate("Operations", successCount, failureCount));
        logger.info(formatSuccessRate("Retries", MetricsRetryListener.getNumRetriesSuccessful(),
                MetricsRetryListener.getNumRetriesFailed()));

        Money totalAfter =
                accountService.getTotalBalance().stream()
                        .filter(money -> money.getCurrency().equals(Money.EUR))
                        .findFirst().orElseThrow(() -> new IllegalStateException("No total found?"));

        Assertions.assertEquals(totalBefore, totalAfter);

        if (failureCount > 0) {
            logger.info("Listing {} execution errors:", executionErrors.size());
            executionErrors.stream().forEach(throwable -> {
                logger.warn(throwable.toString());
            });
        }

        Assertions.assertEquals(0, failureCount);
    }

    @Test
    @Order(3)
    public void whenFundsTransferred_expectTransactionHistory() {
        Page<Transaction> page = transactionService.find(PageRequest.of(0, numAccounts));
        Assertions.assertEquals(numTasks, page.getTotalElements());
    }

    static String formatSuccessRate(String prefix, int success, int failures) {
        return String.format("%s (Total: %d| Success: %d| Failures: %d| Success Rate: %.2f%%)",
                prefix,
                success + failures,
                success,
                failures,
                100 - (failures / (double) (Math.max(1, success + failures))) * 100.0);
    }
}
