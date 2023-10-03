package org.springframework.data.cockroachdb.shell.event;

import org.springframework.context.ApplicationEvent;

public class DisconnectedEvent extends ApplicationEvent {
    public DisconnectedEvent(Object source) {
        super(source);
    }
}
