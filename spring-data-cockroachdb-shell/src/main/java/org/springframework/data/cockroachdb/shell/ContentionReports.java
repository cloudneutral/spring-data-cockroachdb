package org.springframework.data.cockroachdb.shell;

import javax.sql.DataSource;

import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import org.springframework.data.cockroachdb.shell.event.ConnectedEvent;
import org.springframework.data.cockroachdb.shell.support.CockroachShellCommand;
import org.springframework.data.cockroachdb.shell.support.TableUtils;

/**
 * https://www.cockroachlabs.com/docs/stable/crdb-internal.html
 */
@ShellComponent
@ShellCommandGroup(value = ShellGroupNames.GROUP_CONTENTION_REPORTS)
public class ContentionReports extends CockroachShellCommand {
    private DataSource dataSource;

    @EventListener
    public void handle(ConnectedEvent event) {
        this.dataSource = event.getDataSource();
    }

    @ShellMethod(value = "Report number of contended indexes", key = "report-contended-indexes")
    @ShellMethodAvailability("connectedCheck")
    public void reportContentedIndexes() {
        executeAndPrintResult("SELECT * FROM crdb_internal.cluster_contended_indexes "
                + "WHERE database_name = current_database()");
    }

    @ShellMethod(value = "Report number of contended keys", key = "report-contended-keys")
    @ShellMethodAvailability("connectedCheck")
    public void reportContendedKeys() {
        executeAndPrintResult(
                "SELECT table_name, index_name, key, num_contention_events FROM crdb_internal.cluster_contended_keys "
                        + "WHERE database_name = current_database()");
    }

    @ShellMethod(value = "Report number of contended tables", key = "report-contended-tables")
    @ShellMethodAvailability("connectedCheck")
    public void reportContendedTables() {
        executeAndPrintResult(
                "SELECT * FROM crdb_internal.cluster_contended_tables "
                        + "WHERE database_name = current_database()");
    }

    @ShellMethod(value = "Report number of cluster contention events", key = "report-cluster-contention-events")
    @ShellMethodAvailability("connectedCheck")
    public void reportContentionEvents() {
        executeAndPrintResult(
                "SELECT * FROM crdb_internal.cluster_contention_events");
    }

    @ShellMethod(value = "Report number of cluster locks", key = "report-cluster-locks")
    @ShellMethodAvailability("connectedCheck")
    public void reportClusterLocks(
            @ShellOption(value = {"--table", "-t"},
                    help = "the name of the table that includes the key this lock is being acquired on",
                    defaultValue = ShellOption.NULL) String tableName,
            @ShellOption(value = {"--detail"},
                    help = "in addition to --table, show additional information about lockholders, sessions, and waiting queries",
                    defaultValue = "false"
            ) boolean detail) {

        if (tableName != null) {
            if (detail) {
                executeAndPrintResult(
                        "SELECT "
                                + "    sessions.session_id, "
                                + "    sessions.client_address, "
                                + "    sessions.application_name, "
                                + "    locks.txn_id, "
                                + "    queries.query_id AS waiting_query_id, "
                                + "    queries.query AS waiting_query, "
                                + "    locks.lock_key_pretty, "
                                + "    locks.ts, "
                                + "    locks.database_name, "
                                + "    locks.schema_name, "
                                + "    locks.table_name, "
                                + "    locks.lock_strength, "
                                + "    locks.granted, "
                                + "    locks.contended "
                                + "FROM "
                                + "    crdb_internal.cluster_locks AS locks "
                                + "    JOIN crdb_internal.cluster_sessions AS sessions ON "
                                + "            locks.txn_id::STRING = sessions.kv_txn "
                                + "    LEFT JOIN crdb_internal.cluster_queries AS queries ON "
                                + "            locks.txn_id = queries.txn_id "
                                + "WHERE "
                                + "    locks.table_name = ?",
                        ps -> {
                            ps.setString(1, tableName);
                        });
            } else {
                executeAndPrintResult(
                        "SELECT database_name, table_name, txn_id, ts, lock_key_pretty, lock_strength, granted, contended "
                                + "FROM crdb_internal.cluster_locks "
                                + "WHERE table_name = ? AND database_name = current_database()",
                        ps -> {
                            ps.setString(1, tableName);
                        });
            }
        } else {
            executeAndPrintResult(
                    "SELECT database_name, table_name, txn_id, ts, lock_key_pretty, lock_strength, granted, contended "
                            + "FROM crdb_internal.cluster_locks "
                            + "WHERE database_name = current_database()");
        }
    }

    private void executeAndPrintResult(String query) {
        console.information(query);

        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.query(query, rs -> {
            console.success(TableUtils.prettyPrintResultSet(rs));
            return null;
        });
    }

    private void executeAndPrintResult(String query, PreparedStatementSetter pss) {
        console.information(query);

        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.query(query, pss, rs -> {
            console.success(TableUtils.prettyPrintResultSet(rs));
            return null;
        });
    }
}
