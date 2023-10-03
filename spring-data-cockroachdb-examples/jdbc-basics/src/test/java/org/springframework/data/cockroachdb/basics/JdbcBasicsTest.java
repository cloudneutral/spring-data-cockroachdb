package org.springframework.data.cockroachdb.basics;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import javax.sql.DataSource;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("integration-test")
@SpringBootTest(classes = JdbcBasicsConfiguration.class)
public class JdbcBasicsTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private DataSource dataSource;

    @Autowired
    private AccountService accountService;

    private Long accountId;

    @BeforeAll
    public void setupTest() {
        logger.info(new JdbcTemplate(dataSource).queryForObject("select version()", String.class));
        accountService.deleteAll();
    }

    @Test
    @Order(1)
    public void whenCreatingAccount_expectAccountToBePersisted() {
        Assertions.assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "No transaction expected!");

        Account account = accountService.create(
                Account.builder()
                        .withAccountType(AccountType.ASSET)
                        .withBalance(BigDecimal.TEN)
                        .withCurrency("USD")
                        .withName("A name")
                        .build());
        Assertions.assertFalse(account.isNew());
        Assertions.assertNotNull(account);
        Assertions.assertNotNull(account.getId());

        accountId = account.getId();
    }

    @Test
    @Order(2)
    public void whenReadingAccountById_expectAccountEntity() {
        Assertions.assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "No transaction expected!");

        Account account = accountService.findById(accountId);
        Assertions.assertFalse(account.isNew());

        Assertions.assertNotNull(account);
        Assertions.assertNotNull(account.getId());
    }

    @Test
    @Order(2)
    public void whenReadingAccountByName_expectAccountEntity() {
        Assertions.assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "No transaction expected!");

        Account account = accountService.findByName("A name");
        Assertions.assertFalse(account.isNew());

        Assertions.assertNotNull(account);
        Assertions.assertNotNull(account.getId());
    }

    @Test
    @Order(3)
    public void whenUpdatingAccount_expectSuccess() {
        Assertions.assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "No transaction expected!");

        Account account = accountService.findById(accountId);
        Assertions.assertNotNull(account);
        Assertions.assertFalse(account.isNew());
        Assertions.assertFalse(account.isClosed());

        account.setClosed(true);
        account.setBalance(BigDecimal.ONE);

        accountService.update(account);

        account = accountService.findById(accountId);
        Assertions.assertTrue(account.isClosed());
    }

    @Test
    @Order(4)
    public void whenReadingAccountBalance_expectStrongValue() {
        Assertions.assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "No transaction expected!");

        BigDecimal balance = accountService.getBalance(accountId);
        Assertions.assertEquals(BigDecimal.ONE.setScale(2), balance);
    }

    @Test
    @Order(5)
    @Timeout(10)
    public void whenReadingAccountBalance_expectStaleValueFirst_thenStrongValue() throws InterruptedException {
        Assertions.assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "No transaction expected!");

        Assertions.assertNull(accountService.getBalanceSnapshot_Implicit(accountId));
        Assertions.assertNull(accountService.getBalanceSnapshot_Explicit(accountId));

        LocalDateTime ts = new JdbcTemplate(dataSource).queryForObject(
                "select now()-follower_read_timestamp()", LocalDateTime.class);
        long millis = ts.toInstant(ZoneOffset.UTC).toEpochMilli();

        logger.info("Waiting {} ms for closed timestamp", millis);
        Thread.sleep(millis);

        BigDecimal balance = accountService.getBalanceSnapshot_Explicit(accountId);
        Assertions.assertEquals(BigDecimal.ONE.setScale(2), balance);

        balance = accountService.getBalanceSnapshot_Implicit(accountId);
        Assertions.assertEquals(BigDecimal.ONE.setScale(2), balance);
    }
}
