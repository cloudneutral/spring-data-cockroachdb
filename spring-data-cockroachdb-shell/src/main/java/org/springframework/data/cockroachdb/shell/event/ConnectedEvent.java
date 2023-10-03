package org.springframework.data.cockroachdb.shell.event;

import javax.sql.DataSource;

import org.springframework.context.ApplicationEvent;

public class ConnectedEvent extends ApplicationEvent {
    private DataSource dataSource;

    private String connectionPrompt;

    public ConnectedEvent(Object source, DataSource dataSource, String connectionPrompt) {
        super(source);
        this.dataSource = dataSource;
        this.connectionPrompt = connectionPrompt;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public String getConnectionPrompt() {
        return connectionPrompt;
    }
}
