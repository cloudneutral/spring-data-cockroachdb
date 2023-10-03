package org.springframework.data.cockroachdb.aspect;

import java.sql.SQLException;
import java.util.List;

import org.springframework.context.ApplicationEvent;

/**
 * Application event optionally published (via callback) after a retry.
 */
public class RetryEvent extends ApplicationEvent {
    private final String message;

    private final List<SQLException> transientExceptions;

    public RetryEvent(Object source, String message, List<SQLException> transientExceptions) {
        super(source);
        this.message = message;
        this.transientExceptions = transientExceptions;
    }

    public String getMessage() {
        return message;
    }

    public List<SQLException> getTransientExceptions() {
        return transientExceptions;
    }
}
