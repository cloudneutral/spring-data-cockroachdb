package org.springframework.data.cockroachdb;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.postgresql.PGProperty;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.cockroachdb.jdbc.CockroachDataSource;
import io.cockroachdb.jdbc.CockroachDriver;
import io.cockroachdb.jdbc.CockroachProperty;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

public abstract class CockroachPooledDataSource {
    public static final String SQL_TRACE_LOGGER_NAME = "io.cockroachdb.jdbc.SQL_TRACE";

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean autoCommit;

        private String url;

        private String username;

        private String password;

        private boolean traceMethods;

        private boolean traceSQL;

        private int maxPoolSize = 50;

        private int minimumIdle = 25;

        private String slf4jLoggerName = SQL_TRACE_LOGGER_NAME;

        private boolean rewriteBatchedInserts
                = Boolean.valueOf(PGProperty.REWRITE_BATCHED_INSERTS.getDefaultValue());

        private boolean retryTransientErrors
                = Boolean.valueOf(CockroachProperty.RETRY_TRANSIENT_ERRORS.getDefaultValue());

        private boolean implicitSelectForUpdate
                = Boolean.valueOf(CockroachProperty.IMPLICIT_SELECT_FOR_UPDATE.getDefaultValue());

        private int retryMaxAttempts
                = Integer.valueOf(CockroachProperty.RETRY_MAX_ATTEMPTS.getDefaultValue());

        private long retryMaxBackoffTime
                = Long.valueOf(CockroachProperty.RETRY_MAX_BACKOFF_TIME.getDefaultValue());

        private String retryStrategyClassName
                = CockroachProperty.RETRY_STRATEGY_CLASSNAME.getDefaultValue();

        private final Map<String, Object> properties = new LinkedHashMap<>();

        private Consumer<HikariConfig> hikariConfigurer;

        private Builder() {
        }

        public Builder withAutoCommit(boolean autoCommit) {
            this.autoCommit = autoCommit;
            return this;
        }

        public Builder withUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder withTraceMethods(boolean traceMethods) {
            this.traceMethods = traceMethods;
            return this;
        }

        public Builder withTraceSQL(boolean traceSQL) {
            this.traceSQL = traceSQL;
            return this;
        }

        public Builder setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        public Builder setMinimumIdle(int minimumIdle) {
            this.minimumIdle = minimumIdle;
            return this;
        }

        public Builder withRetryTransientErrors(boolean retryTransientErrors) {
            this.retryTransientErrors = retryTransientErrors;
            return this;
        }

        public Builder withImplicitSelectForUpdate(boolean implicitSelectForUpdate) {
            this.implicitSelectForUpdate = implicitSelectForUpdate;
            return this;
        }

        public Builder withRewriteBatchedInserts(boolean rewriteBatchedInserts) {
            this.rewriteBatchedInserts = rewriteBatchedInserts;
            return this;
        }

        public Builder withRetryMaxAttempts(int retryMaxAttempts) {
            this.retryMaxAttempts = retryMaxAttempts;
            return this;
        }

        public Builder withRetryMaxBackoffTime(long retryMaxBackoffTime) {
            this.retryMaxBackoffTime = retryMaxBackoffTime;
            return this;
        }

        public Builder withRetryStrategyClassName(String retryStrategyClassName) {
            this.retryStrategyClassName = retryStrategyClassName;
            return this;
        }

        public Builder withSlf4jLoggerName(String slf4jLoggerName) {
            this.slf4jLoggerName = slf4jLoggerName;
            return this;
        }

        public Builder withDataSourceProperties(Consumer<CockroachDataSource.DataSourceConfig> configurer) {
            configurer.accept(properties::put);
            return this;
        }

        public Builder withHikariConfigurer(Consumer<HikariConfig> hikariConfigurer) {
            this.hikariConfigurer = hikariConfigurer;
            return this;
        }

        public DataSource build() {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName(CockroachDriver.class.getName());

            config.setAutoCommit(autoCommit);
            config.setMaximumPoolSize(maxPoolSize);
            config.setMinimumIdle(minimumIdle);

            if (maxPoolSize != minimumIdle) {
                config.setKeepaliveTime(60000);
                config.setMaxLifetime(1800000);
            }

            config.addDataSourceProperty(PGProperty.REWRITE_BATCHED_INSERTS.getName(),
                    rewriteBatchedInserts);
            config.addDataSourceProperty(CockroachProperty.RETRY_TRANSIENT_ERRORS.getName(),
                    retryTransientErrors);
            config.addDataSourceProperty(CockroachProperty.IMPLICIT_SELECT_FOR_UPDATE.getName(),
                    implicitSelectForUpdate);
            config.addDataSourceProperty(CockroachProperty.RETRY_MAX_ATTEMPTS.getName(),
                    retryMaxAttempts);
            config.addDataSourceProperty(CockroachProperty.RETRY_MAX_BACKOFF_TIME.getName(),
                    retryMaxBackoffTime);
            config.addDataSourceProperty(CockroachProperty.RETRY_STRATEGY_CLASSNAME.getName(),
                    retryStrategyClassName);

            properties.forEach(config::addDataSourceProperty);

            if (hikariConfigurer != null) {
                hikariConfigurer.accept(config);
            }

            if (traceMethods || traceSQL) {
                ProxyDataSourceBuilder builder = ProxyDataSourceBuilder
                        .create(new HikariDataSource(config))
                        .traceMethodsWhen(() -> traceMethods);
                if (traceSQL) {
                    builder.logQueryBySlf4j(SLF4JLogLevel.TRACE, slf4jLoggerName)
                            .asJson()
                            .multiline();
                }
                return builder.build();
            }

            return new HikariDataSource(config);
        }
    }

    private CockroachPooledDataSource() {
    }
}
