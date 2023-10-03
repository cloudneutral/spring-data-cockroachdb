package org.springframework.data.cockroachdb.shell;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.springframework.context.event.EventListener;
import org.springframework.data.cockroachdb.shell.event.ConnectedEvent;
import org.springframework.data.cockroachdb.shell.event.TransactionEndedEvent;
import org.springframework.data.cockroachdb.shell.event.TransactionStartedEvent;
import org.springframework.data.cockroachdb.shell.support.CockroachShellCommand;
import org.springframework.data.cockroachdb.shell.support.TableUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
@ShellCommandGroup(value = ShellGroupNames.GROUP_QUERIES)
public class Query extends CockroachShellCommand {
    private Optional<DataSource> dataSource = Optional.empty();

    private Optional<Connection> transactionalConnection = Optional.empty();

    protected boolean isTransactional() {
        return this.transactionalConnection.isPresent();
    }

    public JdbcTemplate getJdbcTemplate() {
        return new JdbcTemplate(getDataSource());
    }

    public DataSource getDataSource() {
        return dataSource.orElseThrow(() -> new IllegalArgumentException("No data source"));
    }

    public Availability transactionCheck() {
        return connectedCheck().isAvailable() ?
                transactionalConnection.isPresent()
                        ? Availability.available()
                        : Availability.unavailable("no transaction active")
                : Availability.unavailable(connectedCheck().getReason());
    }

    public Availability noTransactionCheck() {
        return connectedCheck().isAvailable() ?
                transactionalConnection.isPresent()
                        ? Availability.unavailable("transaction already active")
                        : Availability.available()
                : Availability.unavailable(connectedCheck().getReason());
    }

    @EventListener
    public void handle(ConnectedEvent event) {
        this.dataSource = Optional.of(event.getDataSource());
    }

    protected void executeAndPrintResult(String query) throws SQLException {
        executeAndPrintResult(query, Collections.emptyList());
    }

    protected void executeAndPrintResult(String query, List<String> params) throws SQLException {
        if (this.transactionalConnection.isPresent()) {
            Connection connection = this.transactionalConnection.get();
            executeAndPrintResult(connection, query, params);
        } else {
            try (Connection connection = getDataSource().getConnection()) {
                connection.setAutoCommit(true);
                executeAndPrintResult(connection, query, params);
            }
        }
    }

    private void executeAndPrintResult(Connection connection, String query, List<String> params) throws SQLException {
        console.information("[%s]", query);

        AtomicInteger n = new AtomicInteger(1);
        params.forEach(p -> console.information("\n\tParam %d [%s]", n.incrementAndGet(), p));

        if (query.toUpperCase().startsWith("SELECT")
                || query.toUpperCase().startsWith("SHOW")) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                for (int i = 0; i < params.size(); i++) {
                    preparedStatement.setString(i + 1, params.get(i));
                }
                try (ResultSet rs = preparedStatement.executeQuery()) {
                    console.success(TableUtils.prettyPrintResultSet(rs));
                }
            }
        } else {
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                for (int i = 0; i < params.size(); i++) {
                    preparedStatement.setString(i + 1, params.get(i));
                }
                int rows = preparedStatement.executeUpdate();
                console.success("Rows affected: %d", rows);
            }
        }
    }

    @ShellMethod(value = "Start new transaction", key = {"begin", "b"})
    @ShellMethodAvailability({"noTransactionCheck"})
    public void begin() throws SQLException {
        if (transactionalConnection.isPresent()) {
            throw new SQLException("Already started transaction");
        }

        Connection c = getDataSource().getConnection();
        c.setAutoCommit(false);

        this.transactionalConnection = Optional.of(c);

        console.information("BEGIN");

        publishEvent(new TransactionStartedEvent(this));
    }

    @ShellMethod(value = "Commit transaction", key = {"commit", "c"})
    @ShellMethodAvailability("transactionCheck")
    public void commit() throws SQLException {
        this.transactionalConnection.orElseThrow(() -> new SQLException("No transaction")).commit();
        this.transactionalConnection = Optional.empty();

        console.information("COMMIT");

        publishEvent(new TransactionEndedEvent(this));
    }

    @ShellMethod(value = "Rollback transaction", key = {"rollback", "r"})
    @ShellMethodAvailability("transactionCheck")
    public void rollback() throws SQLException {
        this.transactionalConnection.orElseThrow(() -> new SQLException("No transaction")).rollback();
        this.transactionalConnection = Optional.empty();

        console.information("ROLLBACK");

        publishEvent(new TransactionEndedEvent(this));
    }

    @ShellMethod(value = "Force retry error", key = {"force-retry", "fr"})
    @ShellMethodAvailability("transactionCheck")
    public void forceRetry(@ShellOption(help = "Force retry interval", defaultValue = "1s") String interval)
            throws SQLException {
        this.transactionalConnection.orElseThrow(() -> new SQLException("No transaction")).rollback();

        String nativeSQL = transactionalConnection.get().nativeSQL(
                "SELECT crdb_internal.force_retry('" + interval + "')");

        console.information(nativeSQL);

        try (Statement statement = transactionalConnection.get().createStatement()) {
            ResultSet resultSet = statement.executeQuery(nativeSQL);
            console.information(TableUtils.prettyPrintResultSet(resultSet));
        }
    }

    @ShellMethod(value = "Execute statement", key = {"execute", "e"})
    @ShellMethodAvailability("connectedCheck")
    public void execute(@ShellOption(value = {"--query"}, help = "SQL query to execute", defaultValue = ShellOption.NULL)
                            String query,
                        @ShellOption(help = "Bind parameter #1", defaultValue = ShellOption.NULL) String param1,
                        @ShellOption(help = "Bind parameter #2", defaultValue = ShellOption.NULL) String param2,
                        @ShellOption(help = "Bind parameter #3", defaultValue = ShellOption.NULL) String param3,
                        @ShellOption(help = "Bind parameter #4", defaultValue = ShellOption.NULL) String param4,
                        @ShellOption(help = "Bind parameter #5", defaultValue = ShellOption.NULL) String param5,
                        @ShellOption(help = "Bind parameter #6", defaultValue = ShellOption.NULL) String param6,
                        @ShellOption(help = "Bind parameter #7", defaultValue = ShellOption.NULL) String param7,
                        @ShellOption(help = "Bind parameter #8", defaultValue = ShellOption.NULL) String param8,
                        @ShellOption(help = "Bind parameter #9", defaultValue = ShellOption.NULL) String param9,
                        @ShellOption(help = "Bind parameter #10", defaultValue = ShellOption.NULL) String param10
    )
            throws SQLException {
        List<String> params = new ArrayList<>();

        Optional.ofNullable(param1).ifPresent(params::add);
        Optional.ofNullable(param2).ifPresent(params::add);
        Optional.ofNullable(param3).ifPresent(params::add);
        Optional.ofNullable(param4).ifPresent(params::add);
        Optional.ofNullable(param5).ifPresent(params::add);
        Optional.ofNullable(param6).ifPresent(params::add);
        Optional.ofNullable(param7).ifPresent(params::add);
        Optional.ofNullable(param8).ifPresent(params::add);
        Optional.ofNullable(param9).ifPresent(params::add);
        Optional.ofNullable(param10).ifPresent(params::add);

        if (query == null) {
            query = askForStringInput("Enter query: ", null, false);
        }

        executeAndPrintResult(query, params);
    }

}
