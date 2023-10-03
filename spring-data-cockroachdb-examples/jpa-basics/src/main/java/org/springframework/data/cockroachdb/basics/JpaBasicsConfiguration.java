package org.springframework.data.cockroachdb.basics;

import javax.sql.DataSource;

import org.postgresql.PGProperty;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.cockroachdb.aspect.AdvisorOrder;
import org.springframework.data.cockroachdb.aspect.TransactionAttributesAspect;
import org.springframework.data.cockroachdb.aspect.TransactionRetryAspect;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariDataSource;

import io.cockroachdb.jdbc.CockroachProperty;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

// Bump up to enable extra advisors with lower priority
@EnableTransactionManagement(order = AdvisorOrder.TRANSACTION_ADVISOR)
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class
})
@EnableJpaRepositories(basePackageClasses = JpaBasicsConfiguration.class)
@ComponentScan(basePackageClasses = JpaBasicsConfiguration.class)
@Configuration
public class JpaBasicsConfiguration {
    @Bean
    public TransactionRetryAspect transactionRetryAspect() {
        return new TransactionRetryAspect();
    }

    @Bean
    public TransactionAttributesAspect transactionAttributesAspect(JdbcTemplate jdbcTemplate) {
        return new TransactionAttributesAspect(jdbcTemplate);
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource() {
        HikariDataSource ds = dataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
        ds.addDataSourceProperty(PGProperty.REWRITE_BATCHED_INSERTS.getName(), "true");

        ds.addDataSourceProperty(CockroachProperty.IMPLICIT_SELECT_FOR_UPDATE.getName(), "true");
        ds.addDataSourceProperty(CockroachProperty.RETRY_TRANSIENT_ERRORS.getName(), "true");
        ds.addDataSourceProperty(CockroachProperty.RETRY_MAX_ATTEMPTS.getName(), "10");
        ds.addDataSourceProperty(CockroachProperty.RETRY_MAX_BACKOFF_TIME.getName(), "15000");

        return ProxyDataSourceBuilder
                .create(ds)
                .name("SQL-Trace")
                .asJson()
                .logQueryBySlf4j(SLF4JLogLevel.TRACE, "io.cockroachdb.jdbc.SQL_TRACE")
                .multiline()
                .build();
    }
}
