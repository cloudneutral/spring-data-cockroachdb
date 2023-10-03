package org.springframework.data.cockroachdb.it.bank;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.cockroachdb.it.bank.model.Account;
import org.springframework.data.cockroachdb.it.bank.model.Money;
import org.springframework.data.cockroachdb.it.bank.model.Transaction;
import org.springframework.data.cockroachdb.it.bank.model.TransferRequest;
import org.springframework.data.cockroachdb.it.util.RandomUtils;
import org.springframework.data.cockroachdb.it.util.TextUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Tag("functional-test")
public class FunctionalBankTest extends AbstractBankIntegrationTest {
    @BeforeAll
    public void setupTestFixture() {
        transactionService.deleteAll();
        accountService.deleteAll();
    }

    private final int numAccounts = 100;

    private final Money initialBalanceEUR = Money.of("50000.00", "EUR");

    private final Money initialBalanceUSD = Money.of("25000.00", "USD");

    private List<Money> initialTotalBalance;

    @Test
    @Order(1)
    public void whenCreatingAccounts_expectTotalBalanceToMatch() {
        accountService.createAccounts("eu", initialBalanceEUR, numAccounts, 512, n -> {
            System.out.printf("\r%s", TextUtils.progressBar(numAccounts, n));
        });
        accountService.createAccounts("us", initialBalanceUSD, numAccounts, 512, n -> {
            System.out.printf("\r%s", TextUtils.progressBar(numAccounts, n));
        });

        this.initialTotalBalance = accountService.getTotalBalance();

        Money eurTotal =
                initialTotalBalance.stream()
                        .filter(money -> money.getCurrencyCode().equals("EUR"))
                        .findFirst().orElseThrow(() -> new IllegalStateException("No total found?"));

        Assertions.assertEquals(initialBalanceEUR.multiply(numAccounts), eurTotal);

        Money usdTotal =
                initialTotalBalance.stream()
                        .filter(money -> money.getCurrencyCode().equals("USD"))
                        .findFirst().orElseThrow(() -> new IllegalStateException("No total found?"));

        Assertions.assertEquals(initialBalanceUSD.multiply(numAccounts), usdTotal);
    }

    @Test
    @Order(3)
    public void whenTransferFunds_expectBalancedOutcome() {
        List<Account> accounts = accountService.findAccountsByRegion("eu", 0, 10_000);
        Set<Account> accountsUsed = new HashSet<>();

        UUID idempotencyKey = UUID.randomUUID();

        TransferRequest.Builder builder = TransferRequest.builder()
                .withId(idempotencyKey)
                .withRegion("eu")
                .withBookingDate(LocalDate.now())
                .withTransferDate(LocalDate.now())
                .withTransactionType("gen");

        IntStream.rangeClosed(1, 2).forEach(value -> {
            Money amount = RandomUtils.randomMoneyBetween(1.00, 150.00, "EUR");

            builder
                    .withRegion("eu")
                    .addLeg()
                    .withId(RandomUtils.selectRandomUnique(accounts, accountsUsed).getId())
                    .withAmount(amount)
                    .withNote("Credit note")
                    .then();

            builder
                    .withRegion("eu")
                    .addLeg()
                    .withId(RandomUtils.selectRandomUnique(accounts, accountsUsed).getId())
                    .withAmount(amount.negate())
                    .withNote("Debit note")
                    .then();
        });

        List<Money> totals = accountService.getTotalBalance();

        Money totalBefore =
                totals.stream()
                        .filter(money -> money.getCurrencyCode().equals("EUR"))
                        .findFirst().orElseThrow(() -> new IllegalStateException("No total found?"));

        Transaction t1 = transactionService.submitTransferRequest(builder.build());
        Assertions.assertNotNull(t1.getId());
        Assertions.assertEquals(4, t1.getItems().size());

        Transaction t2 = transactionService.submitTransferRequest(builder.build());
        Assertions.assertEquals(t1, t2);

        totals = accountService.getTotalBalance();

        Money totalAfter =
                totals.stream()
                        .filter(money -> money.getCurrencyCode().equals("EUR"))
                        .findFirst().orElseThrow(() -> new IllegalStateException("No total found?"));

        Assertions.assertEquals(totalBefore, totalAfter);

        Page<Transaction> page = transactionService.find(PageRequest.of(0, 10_000));
        Assertions.assertEquals(1, page.getTotalElements());
    }

    @Test
    @Order(4)
    public void whenTransferFundsCompleted_thenReportSummary() throws InterruptedException {
        List<Money> currentTotalBalance = accountService.getTotalBalance();

        Money eurTotal =
                currentTotalBalance.stream()
                        .filter(money -> money.getCurrencyCode().equals("EUR"))
                        .findFirst().orElseThrow(() -> new IllegalStateException("No total found?"));

        Assertions.assertEquals(initialBalanceEUR.multiply(numAccounts), eurTotal);

        Money usdTotal =
                currentTotalBalance.stream()
                        .filter(money -> money.getCurrencyCode().equals("USD"))
                        .findFirst().orElseThrow(() -> new IllegalStateException("No total found?"));

        Assertions.assertEquals(initialBalanceUSD.multiply(numAccounts), usdTotal);

        // Wait due to follower reads
        Thread.sleep(5000);
        logger.info(reportingService.accountSummary("eu").toString());
        logger.info(reportingService.accountSummary("us").toString());
    }
}
