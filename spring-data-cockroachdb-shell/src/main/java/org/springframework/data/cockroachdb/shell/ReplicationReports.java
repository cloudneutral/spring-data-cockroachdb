package org.springframework.data.cockroachdb.shell;

import java.sql.SQLException;

import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

/**
 * https://www.cockroachlabs.com/docs/stable/query-replication-reports.html
 */
@ShellComponent
@ShellCommandGroup(value = ShellGroupNames.GROUP_REPLICATION_REPORTS)
public class ReplicationReports extends Query {
    @ShellMethod(value = "Report replication metadata", key = "report-replication-metadata")
    @ShellMethodAvailability("connectedCheck")
    public void reportReplicationMeta() throws SQLException {
        executeAndPrintResult("SELECT * FROM system.reports_meta");
    }

    @ShellMethod(value = "Report data that is under-replicated, over-replicated, or unavailable", key = "report-replication-stats")
    @ShellMethodAvailability("connectedCheck")
    public void reportReplicationStats(@ShellOption(value = {
            "--violating"}, help = "filter on violating ranges", defaultValue = "false") boolean violatingRanges,
                                       @ShellOption(value = {
                                               "--under-replicated"}, help = "filter on under-replicated ranges", defaultValue = "false") boolean underReplicated)
            throws SQLException {
        if (violatingRanges) {
            executeAndPrintResult("WITH "
                    + "    partition_violations "
                    + "        AS ( "
                    + "            SELECT "
                    + "                * "
                    + "            FROM "
                    + "                system.replication_constraint_stats "
                    + "            WHERE "
                    + "                violating_ranges > 0 "
                    + "        ), "
                    + "    report "
                    + "        AS ( "
                    + "            SELECT "
                    + "                crdb_internal.zones.zone_id, "
                    + "                crdb_internal.zones.subzone_id, "
                    + "                target, "
                    + "                database_name, "
                    + "                table_name, "
                    + "                index_name, "
                    + "                partition_violations.type, "
                    + "                partition_violations.config, "
                    + "                partition_violations.violation_start, "
                    + "                partition_violations.violating_ranges "
                    + "            FROM "
                    + "                crdb_internal.zones, partition_violations "
                    + "            WHERE "
                    + "                crdb_internal.zones.zone_id "
                    + "                = partition_violations.zone_id "
                    + "        ) "
                    + "SELECT * FROM report");
        } else if (underReplicated) {
            executeAndPrintResult("WITH "
                    + "    under_replicated_zones "
                    + "        AS ( "
                    + "            SELECT "
                    + "                zone_id, under_replicated_ranges "
                    + "            FROM "
                    + "                system.replication_stats "
                    + "            WHERE "
                    + "                under_replicated_ranges > 0 "
                    + "        ), "
                    + "    report "
                    + "        AS ( "
                    + "            SELECT "
                    + "                crdb_internal.zones.zone_id, "
                    + "                target, "
                    + "                range_name, "
                    + "                database_name, "
                    + "                table_name, "
                    + "                index_name, "
                    + "                under_replicated_zones.under_replicated_ranges "
                    + "            FROM "
                    + "                crdb_internal.zones, under_replicated_zones "
                    + "            WHERE "
                    + "                crdb_internal.zones.zone_id "
                    + "                = under_replicated_zones.zone_id "
                    + "        ) "
                    + "SELECT * FROM report");
        } else {
            executeAndPrintResult("SELECT * FROM system.replication_stats");
        }
    }

    @ShellMethod(value = "Report data placement constraints", key = "report-replication-constraints")
    @ShellMethodAvailability("connectedCheck")
    public void reportReplicationConstraints() throws SQLException {
        executeAndPrintResult("SELECT * FROM system.replication_constraint_stats");
    }

    @ShellMethod(value = "Report which localities (if any) that are critical", key = "report-replication-critical")
    @ShellMethodAvailability("connectedCheck")
    public void reportReplicationCritical(@ShellOption(value = {
            "--at-risk"}, help = "filter on at risk ranges", defaultValue = "false") boolean atRisk)
            throws SQLException {
        if (atRisk) {
            executeAndPrintResult("WITH "
                    + "    at_risk_zones AS ( "
                    + "            SELECT "
                    + "                zone_id, locality, at_risk_ranges "
                    + "            FROM "
                    + "                system.replication_critical_localities "
                    + "            WHERE "
                    + "                at_risk_ranges > 0 "
                    + "        ), "
                    + "    report AS ( "
                    + "            SELECT "
                    + "                crdb_internal.zones.zone_id, "
                    + "                target, "
                    + "                database_name, "
                    + "                table_name, "
                    + "                index_name, "
                    + "                at_risk_zones.at_risk_ranges "
                    + "            FROM "
                    + "                crdb_internal.zones, at_risk_zones "
                    + "            WHERE "
                    + "                crdb_internal.zones.zone_id "
                    + "                = at_risk_zones.zone_id "
                    + "        ) "
                    + "SELECT DISTINCT * FROM report");
        } else {
            executeAndPrintResult("SELECT * FROM system.replication_critical_localities");
        }
    }

    @ShellMethod(value = "Report where data lives", key = "report-replication-zones")
    @ShellMethodAvailability("connectedCheck")
    public void reportReplicationZones() throws SQLException {
        executeAndPrintResult("SELECT * FROM crdb_internal.zones");
    }
}
