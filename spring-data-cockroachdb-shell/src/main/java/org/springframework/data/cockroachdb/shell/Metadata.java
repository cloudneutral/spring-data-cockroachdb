package org.springframework.data.cockroachdb.shell;

import java.lang.reflect.InvocationTargetException;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Predicate;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.shell.standard.EnumValueProvider;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.ReflectionUtils;

import org.springframework.data.cockroachdb.annotations.Variable;

@ShellComponent
@ShellCommandGroup(value = ShellGroupNames.GROUP_METADATA)
public class Metadata extends Query {
    @ShellMethod(value = "Print database metadata", key = {"metadata", "m"})
    @ShellMethodAvailability("connectedCheck")
    public void metadata(@ShellOption(help = "Include all no-arg database metadata methods", defaultValue = "false") boolean all) {
        getJdbcTemplate().execute((ConnectionCallback<Object>) connection -> {
            DatabaseMetaData metaData = connection.getMetaData();

            Map<String, Object> properties = new TreeMap<>();
            if (all) {
                ReflectionUtils.doWithMethods(java.sql.DatabaseMetaData.class, method -> {
                    if (method.getParameterCount() == 0) {
                        try {
                            Object rv = method.invoke(metaData);
                            properties.put(method.getName(), rv);
                        } catch (InvocationTargetException e) {
                            console.warning(e.getTargetException().getMessage());
                        }
                    }
                });
            } else {
                properties.put("databaseProductName", metaData.getDatabaseProductName());
                properties.put("databaseMajorVersion", metaData.getDatabaseMajorVersion());
                properties.put("databaseMinorVersion", metaData.getDatabaseMinorVersion());
                properties.put("databaseProductVersion", metaData.getDatabaseProductVersion());
                properties.put("driverMajorVersion", metaData.getDriverMajorVersion());
                properties.put("driverMinorVersion", metaData.getDriverMinorVersion());
                properties.put("driverName", metaData.getDriverName());
                properties.put("driverVersion", metaData.getDriverVersion());
                properties.put("maxConnections", metaData.getMaxConnections());
                properties.put("defaultTransactionIsolation", metaData.getDefaultTransactionIsolation());
                properties.put("transactionIsolation", connection.getTransactionIsolation());
            }

            properties.forEach((k, v) -> console.information("%s = %s", k, v));
            return null;
        });
    }

    @ShellMethod(value = "Print database version", key = {"version", "v"})
    @ShellMethodAvailability("connectedCheck")
    public void version() {
        console.information(getJdbcTemplate().queryForObject("select version()", String.class));
    }

    @ShellMethod(value = "Print current database", key = {"current-database"})
    @ShellMethodAvailability("connectedCheck")
    public void currentDatabase() {
        console.information(getJdbcTemplate().queryForObject("select current_database()", String.class));
    }

    @ShellMethod(value = "Print current user", key = {"current-user"})
    @ShellMethodAvailability("connectedCheck")
    public void currentUser() {
        console.information(getJdbcTemplate().queryForObject("select current_user()", String.class));
    }

    @ShellMethod(value = "Print gateway region", key = {"gateway-region"})
    @ShellMethodAvailability("connectedCheck")
    public void gatewayRegion() {
        console.information(getJdbcTemplate().queryForObject("select gateway_region()", String.class));
    }

    @ShellMethod(value = "Print current node region", key = {"rehome-row"})
    @ShellMethodAvailability("connectedCheck")
    public void rehomeRow() {
        console.information(getJdbcTemplate().queryForObject("select rehome_row()", String.class));
    }

    @ShellMethod(value = "Set session variable", key = {"set"}, group = ShellGroupNames.GROUP_SETTINGS)
    @ShellMethodAvailability("connectedCheck")
    public void setVariable(
            @ShellOption(help = "Session variable to set",
                    valueProvider = EnumValueProvider.class) Variable variable,
            @ShellOption(help = "value to set", defaultValue = ShellOption.NULL) String value
    ) throws SQLException {
        if (value == null) {
            value = askForStringInput("Enter new value: ", variable.getDefaultValue(), false);
        }

        if (isTransactional()) {
            executeAndPrintResult("SET LOCAL " + variable.name() + " = " + value);
        } else {
            executeAndPrintResult("SET SESSION " + variable.name() + " = " + value);
        }
    }

    @ShellMethod(value = "Show all session variables", key = {"show-all"}, group = ShellGroupNames.GROUP_SETTINGS)
    @ShellMethodAvailability("connectedCheck")
    public void showVariables(
            @ShellOption(help = "Include mutable variables only", defaultValue = "false") boolean mutable,
            @ShellOption(help = "Include official variables only", defaultValue = "false") boolean official,
            @ShellOption(help = "Include modified variables only", defaultValue = "false") boolean modified
    ) {
        printVariables(variable -> (!mutable || variable.isMutable())
                && (!official || variable.isOfficial()), variable -> !modified);
    }

    private void printVariables(Predicate<Variable> predicate, Predicate<Variable> includeUnmodified) {
        getJdbcTemplate().query("select variable,value from [show all]", rs -> {
            String variable = rs.getString(1);
            String value = rs.getString(2);
            try {
                Variable v = Variable.valueOf(variable);
                if (predicate.test(v)) {
                    String defaultValue = v.getDefaultValue();
                    if (!Objects.equals(value, defaultValue)) {
                        console.warning("%s = '%s' (default is: '%s')", variable, value, defaultValue);
                    } else {
                        if (includeUnmodified.test(v)) {
                            console.information("%s = %s", variable, value);
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                console.error("(UNKNOWN) %s = %s", variable, value);
            }
        });
    }

    @ShellMethod(value = "Show session variable", key = {"show"}, group = ShellGroupNames.GROUP_SETTINGS)
    @ShellMethodAvailability("connectedCheck")
    public void showVariable(
            @ShellOption(help = "Session variable to show",
                    valueProvider = EnumValueProvider.class) Variable variable
    ) throws SQLException {
        if (isTransactional()) {
            executeAndPrintResult("SHOW " + variable.name());
        } else {
            executeAndPrintResult("SHOW SESSION " + variable.name());
        }

        console.information("Variable '%s' details:", variable.name());
        console.information("  description: %s", variable.getDescription());
        console.information("  mutable: %s", variable.isMutable());
        console.information("  official: %s", variable.isOfficial());
        console.information("  defaultValue: %s", variable.getDefaultValue());
        console.information("  since: %s", variable.getVersion());
    }

    @ShellMethod(value = "Show cluster setting(s)", key = {
            "show-cluster-setting"}, group = ShellGroupNames.GROUP_SETTINGS)
    @ShellMethodAvailability("connectedCheck")
    public void showSetting(
            @ShellOption(help = "Cluster setting to show",
                    valueProvider = ClusterSettingProvider.class, defaultValue = ShellOption.NULL) String name
    ) throws SQLException {
        if (name == null) {
            executeAndPrintResult("SHOW ALL CLUSTER SETTINGS");
        } else {
            executeAndPrintResult("SHOW CLUSTER SETTING " + name);
        }
    }

    @ShellMethod(value = "Set cluster setting", key = {"set-cluster-setting"}, group = ShellGroupNames.GROUP_SETTINGS)
    @ShellMethodAvailability("connectedCheck")
    public void setSetting(
            @ShellOption(help = "Cluster setting to set",
                    valueProvider = ClusterSettingProvider.class) String name,
            @ShellOption(help = "value to set", defaultValue = ShellOption.NULL) String value
    ) {
        String finalValue;
        if (value == null) {
            String currentValue = getJdbcTemplate().queryForObject("SHOW CLUSTER SETTING " + name, String.class);
            finalValue = askForStringInput("Enter new value: ", currentValue, false);
        } else {
            finalValue = value;
        }

        getJdbcTemplate().update("SET CLUSTER SETTING " + name + " = " + finalValue);
    }
}
