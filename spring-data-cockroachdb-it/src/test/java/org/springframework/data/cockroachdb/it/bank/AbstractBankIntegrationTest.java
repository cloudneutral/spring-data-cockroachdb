package org.springframework.data.cockroachdb.it.bank;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.cockroachdb.it.bank.service.AccountService;
import org.springframework.data.cockroachdb.it.bank.service.ReportingService;
import org.springframework.data.cockroachdb.it.bank.service.TransactionService;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

@SpringBootTest(classes = BankApplication.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("integration-test")
//@ActiveProfiles({"jpa","dev"})
//@ActiveProfiles({"jdbc","dev"})
public abstract class AbstractBankIntegrationTest {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected AccountService accountService;

    @Autowired
    protected TransactionService transactionService;

    @Autowired
    protected ReportingService reportingService;

    @Autowired
    private HikariDataSource hikariDataSource;

    protected void logPoolStats() {
        HikariConfigMXBean config = hikariDataSource.getHikariConfigMXBean();
        logger.info("Hikari pool config:\n"
                        + "maxLifetime: {}\n"
                        + "connectionTimeout: {}\n"
                        + "validationTimeout: {}\n"
                        + "minimumIdle: {}\n"
                        + "maximumPoolSize: {}",
                config.getMaxLifetime(),
                config.getConnectionTimeout(),
                config.getValidationTimeout(),
                config.getMinimumIdle(),
                config.getMaximumPoolSize()
        );
        HikariPoolMXBean pool = hikariDataSource.getHikariPoolMXBean();
        logger.info("Hikari pool:\n"
                        + "activeConnections: {}\n"
                        + "idleConnections: {}\n"
                        + "threadsAwaitingConnection: {}\n"
                        + "totalConnections: {}",
                pool.getActiveConnections(),
                pool.getIdleConnections(),
                pool.getThreadsAwaitingConnection(),
                pool.getTotalConnections()
        );
    }
}
