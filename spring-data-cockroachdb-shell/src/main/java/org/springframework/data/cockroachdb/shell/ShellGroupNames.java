package org.springframework.data.cockroachdb.shell;

public abstract class ShellGroupNames {
    public static final String GROUP_CONNECTION = "1. Connection";

    public static final String GROUP_METADATA = "2. Metadata";

    public static final String GROUP_SETTINGS = "3. Settings";

    public static final String GROUP_CONTENTION_REPORTS = "4. Contention Reports";

    public static final String GROUP_REPLICATION_REPORTS = "5. Replication Reports";

    public static final String GROUP_QUERIES = "6. Queries";

    private ShellGroupNames() {
    }
}
