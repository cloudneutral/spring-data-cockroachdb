package org.springframework.data.cockroachdb.it.bank;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cockroachdb.aspect.AdvisorOrder;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

// Bump up to enable extra advisors with lower priority
@EnableTransactionManagement(order = AdvisorOrder.TRANSACTION_ADVISOR)
@EnableAutoConfiguration
@EnableConfigurationProperties
@EnableJpaRepositories(basePackageClasses = BankApplication.class, enableDefaultTransactions = false)
@ComponentScan(basePackageClasses = BankApplication.class)
@Configuration
public class BankApplication {
}
