package org.springframework.data.cockroachdb.shell;

import java.util.function.Consumer;

import javax.sql.DataSource;

import org.postgresql.PGProperty;
import org.springframework.data.cockroachdb.CockroachPooledDataSource;
import org.springframework.data.cockroachdb.shell.event.ConnectedEvent;
import org.springframework.data.cockroachdb.shell.event.DisconnectedEvent;
import org.springframework.data.cockroachdb.shell.support.CockroachShellCommand;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.StringUtils;

import io.cockroachdb.jdbc.CockroachDataSource;
import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

@ShellComponent
@ShellCommandGroup(value = ShellGroupNames.GROUP_CONNECTION)
public class Connect extends CockroachShellCommand {
    @ShellMethodAvailability("notConnectedCheck")
    @ShellMethod(value = "Connect to CockroachDB", key = {"connect", "co"})
    public void connect(
            @ShellOption(value = {"--host", "-t"}, help = "hostname", defaultValue = "localhost") String host,
            @ShellOption(value = {"--port", "-p"}, help = "listen port", defaultValue = "26257") String port,
            @ShellOption(value = {"--database", "-d"}, help = "database name", defaultValue = "defaultdb") String database,
            @ShellOption(value = {"--username","-u"}, help = "username used to connect to database", defaultValue = "root") String username,
            @ShellOption(help = "password used to connect to database", defaultValue = ShellOption.NULL) String password,
            @ShellOption(help = "SSL mode for database connection (disable, allow, prefer, require, verify-ca, verify-full)", defaultValue = "disable") String sslMode,
            @ShellOption(help = "path to Root Cert file when SSL is enabled", defaultValue = ShellOption.NULL) String sslRootCrtPath,
            @ShellOption(help = "path to SSL Client Cert file when SSL is enabled", defaultValue = ShellOption.NULL) String sslClientCrtPath,
            @ShellOption(help = "path to SSL Client Key file when SSL is enabled", defaultValue = ShellOption.NULL) String sslClientKeyPath,
            @ShellOption(value = {"--sql-trace", "-e"}, help = "enable method and SQL tracing", defaultValue = "false") boolean traceSQL,
            @ShellOption(value = {"--trace","-t"}, help = "enable JDBC API method tracing", defaultValue = "false") boolean traceMethods,
            @ShellOption(value = {"--internal-retry","-y"}, defaultValue = "false", help = "Enable driver-level retries") boolean internalRetry,
            @ShellOption(value = {"--implicit-sfu","-u"}, defaultValue = "false", help = "Enable implicit SELECT FOR UPDATE rewrite") boolean implicitSFU,
            @ShellOption(value = {"--pool-size"}, defaultValue = "0", help = "If non-zero, use a pooled data source with given max/min size. Otherwise a non-pooled data source") int poolSize,
            @ShellOption(value = {"--dev"}, defaultValue = "false", help = "Internal") boolean devHost) {
        if (devHost) {
            host = "192.168.1.99";
        }

        String url = String.format("jdbc:cockroachdb://%s:%s/%s", host, port, database);

        console.information("Connecting to '%s'", url);

        DataSource dataSource;

        Consumer<CockroachDataSource.DataSourceConfig> configurer = props -> {
            props.addDataSourceProperty(PGProperty.SSL_MODE.getName(), sslMode);
            if (!sslMode.equals("disable")) {
                props.addDataSourceProperty(PGProperty.SSL.getName(), "true");
                if (StringUtils.hasText(sslRootCrtPath)) {
                    props.addDataSourceProperty(PGProperty.SSL_ROOT_CERT.getName(), sslRootCrtPath);
                }
                if (StringUtils.hasText(sslClientCrtPath)) {
                    props.addDataSourceProperty(PGProperty.SSL_CERT.getName(), sslClientCrtPath);
                }
                if (StringUtils.hasText(sslClientKeyPath)) {
                    props.addDataSourceProperty(PGProperty.SSL_KEY.getName(), sslClientKeyPath);
                }
            }
        };

        if (poolSize > 0) {
            dataSource = CockroachPooledDataSource
                    .builder()
                    .withUrl(url)
                    .withUsername(username)
                    .withPassword(password)
                    .withAutoCommit(true)
                    .withImplicitSelectForUpdate(implicitSFU)
                    .withRetryTransientErrors(internalRetry)
                    .withDataSourceProperties(configurer)

                    .withTraceMethods(traceMethods)
                    .withTraceSQL(traceSQL)
                    .withHikariConfigurer(config -> {
                        config.setMaximumPoolSize(25);
                        config.setMinimumIdle(5);
                        config.setConnectionTimeout(3000);
                        config.setInitializationFailTimeout(3000);
                    })
                    .build();
        } else {
            dataSource = CockroachDataSource
                    .builder()
                    .withUrl(url)
                    .withUsername(username)
                    .withPassword(password)
                    .withAutoCommit(true)
                    .withImplicitSelectForUpdate(implicitSFU)
                    .withRetryTransientErrors(internalRetry)
                    .withDataSourceProperties(configurer)
                    .build();

            // Non-pooled DS doesnt have ttddyy dep
            if (traceMethods || traceSQL) {
                ProxyDataSourceBuilder builder = ProxyDataSourceBuilder
                        .create(dataSource)
                        .traceMethodsWhen(() -> traceMethods);
                if (traceSQL) {
                    builder.logQueryBySlf4j(SLF4JLogLevel.TRACE, CockroachPooledDataSource.SQL_TRACE_LOGGER_NAME)
                            .asJson()
                            .multiline();
                }
                dataSource = builder.build();
            }
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        console.information("Connected to %s", jdbcTemplate.queryForObject("select version()", String.class));

        CockroachShellCommand.setConnected(true);
        publishEvent(new ConnectedEvent(this, dataSource, username + "@" + host + ":" + port + "/" + database));
    }

    @ShellMethod(value = "Disconnect from CockroachDB", key = {"disconnect", "di"})
    @ShellMethodAvailability("connectedCheck")
    public void disconnect() {
        console.information("Closing connection");
        CockroachShellCommand.setConnected(false);
        publishEvent(new DisconnectedEvent(this));
    }
}
