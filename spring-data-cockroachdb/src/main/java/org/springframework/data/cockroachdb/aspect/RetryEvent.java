package org.springframework.data.cockroachdb.aspect;

import java.sql.SQLException;
import java.time.Duration;
import java.util.List;

import org.springframework.context.ApplicationEvent;

/**
 * Application event optionally published (via callback) after a retry.
 */
public class RetryEvent extends ApplicationEvent {
    private final SQLException sqlException;

    private final int numCalls;

    private final String methodName;

    private final Duration elapsedTime;

    private final String message;

    public RetryEvent(Object source, SQLException sqlException, int numCalls, String methodName, Duration elapsedTime,
                      String message) {
        super(source);
        this.sqlException = sqlException;
        this.numCalls = numCalls;
        this.methodName = methodName;
        this.elapsedTime = elapsedTime;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public SQLException getSqlException() {
        return sqlException;
    }

    public int getNumCalls() {
        return numCalls;
    }

    public String getMethodName() {
        return methodName;
    }

    public Duration getElapsedTime() {
        return elapsedTime;
    }
}
