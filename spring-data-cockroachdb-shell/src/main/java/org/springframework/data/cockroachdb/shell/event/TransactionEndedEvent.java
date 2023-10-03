package org.springframework.data.cockroachdb.shell.event;

import org.springframework.context.ApplicationEvent;

public class TransactionEndedEvent extends ApplicationEvent {
    public TransactionEndedEvent(Object source) {
        super(source);
    }
}
