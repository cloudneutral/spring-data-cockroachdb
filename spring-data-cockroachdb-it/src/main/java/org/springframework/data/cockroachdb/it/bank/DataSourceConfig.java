package org.springframework.data.cockroachdb.it.bank;

import javax.sql.DataSource;

import org.postgresql.PGProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.cockroachdb.it.TestProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.zaxxer.hikari.HikariDataSource;

import io.cockroachdb.jdbc.CockroachProperty;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

@Configuration
public class DataSourceConfig {
    public static final String SQL_TRACE_LOGGER = "io.cockroachdb.jdbc.SQL_TRACE";

    @Autowired
    private Environment environment;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariDataSource hikariDataSource = hikariDataSource();
        return ProxyDataSourceBuilder.create(hikariDataSource)
                .name("SQL-Trace").asJson()
                .logQueryBySlf4j(SLF4JLogLevel.TRACE, SQL_TRACE_LOGGER)
                .build();
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties getDatasourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource hikariDataSource() {
        HikariDataSource ds = getDatasourceProperties()
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        ds.setMaximumPoolSize(Runtime.getRuntime().availableProcessors() * 30);
        ds.setMinimumIdle(Runtime.getRuntime().availableProcessors() * 15);
        ds.setConnectionTimeout(5_000); // Max wait to get connection from pool (millis)
        ds.setInitializationFailTimeout(-1); // Max wait to get connection from pool (millis)
        ds.setAutoCommit(true);
        ds.setPoolName("Spring Data CockroachDB");

        ds.addDataSourceProperty(PGProperty.REWRITE_BATCHED_INSERTS.getName(), "true");

        if (!environment.acceptsProfiles(Profiles.of(TestProfiles.APP_RETRY))) {
            ds.addDataSourceProperty(CockroachProperty.IMPLICIT_SELECT_FOR_UPDATE.getName(), "true");
            ds.addDataSourceProperty(CockroachProperty.RETRY_CONNECTION_ERRORS.getName(), "true");
            ds.addDataSourceProperty(CockroachProperty.RETRY_TRANSIENT_ERRORS.getName(), "true");
            ds.addDataSourceProperty(CockroachProperty.RETRY_MAX_ATTEMPTS.getName(), "10");
            ds.addDataSourceProperty(CockroachProperty.RETRY_MAX_BACKOFF_TIME.getName(), "15s");
            ds.addDataSourceProperty(CockroachProperty.RETRY_LISTENER_CLASSNAME.getName(),
                    MetricsRetryListener.class.getName());
        }

        return ds;
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(dataSource());
    }
}
