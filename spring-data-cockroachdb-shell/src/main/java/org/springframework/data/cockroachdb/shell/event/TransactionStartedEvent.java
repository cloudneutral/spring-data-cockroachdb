package org.springframework.data.cockroachdb.shell.event;

import org.springframework.context.ApplicationEvent;

public class TransactionStartedEvent extends ApplicationEvent {
    public TransactionStartedEvent(Object source) {
        super(source);
    }
}
