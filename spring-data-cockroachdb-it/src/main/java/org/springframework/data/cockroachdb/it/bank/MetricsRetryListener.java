package org.springframework.data.cockroachdb.it.bank;

import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import io.cockroachdb.jdbc.retry.LoggingRetryListener;

public class MetricsRetryListener extends LoggingRetryListener {
    private static final AtomicInteger numRetriesSuccessful = new AtomicInteger();

    private static final AtomicInteger numRetriesFailed = new AtomicInteger();

    @Override
    public void afterRetry(String methodName, int attempt, SQLException ex, Duration executionTime) {
        super.afterRetry(methodName, attempt, ex, executionTime);
        if (ex != null) {
            numRetriesFailed.incrementAndGet();
        } else {
            numRetriesSuccessful.incrementAndGet();
        }
    }

    public static void reset() {
        numRetriesSuccessful.set(0);
        numRetriesFailed.set(0);
    }

    public static int getNumRetriesSuccessful() {
        return numRetriesSuccessful.get();
    }

    public static int getNumRetriesFailed() {
        return numRetriesFailed.get();
    }

//    public List<Duration> getExecutionTimes() {
//        return new ArrayList<>(executionTimes);
//    }
//
//    public Duration getTotalExecutionTime() {
//        Duration total = Duration.ZERO;
//        for (Duration duration : getExecutionTimes()) {
//            total = total.plus(duration);
//        }
//        return total;
//    }
}
