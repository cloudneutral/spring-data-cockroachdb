package org.springframework.data.cockroachdb.aspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.UndeclaredThrowableException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.postgresql.util.PSQLState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import org.springframework.data.cockroachdb.annotations.Retryable;

/**
 * AOP aspect that automatically retries operations that throw transient SQL exceptions
 * with state code 40001. It applies an around-advice for all annotated methods and
 * intercepts and retries concurrency failures such as deadlock looser,
 * pessimistic and optimistic locking failures.
 * <p>
 * Concurrency related failures are more common for databases running in higher isolation
 * levels such as 1SR when a workload is contended (interleaved RW, WR or WW operations
 * that don't serialize).
 * <p>
 * This aspect is separate from {@link TransactionAttributesAspect} which allows it to
 * be used directly with {@link org.springframework.transaction.annotation.Transactional}
 * using propagation attribute {@link org.springframework.transaction.annotation.Propagation#REQUIRES_NEW}.
 * The main pre-condition is that no existing transaction can be in scope when attempting
 * a retry, hence it's applicable for transaction boundaries only. The business operation
 * retried must also be idempotent since it can be invoked more than once due to a retry.
 * <p>
 * This advice must be applied before the Spring transaction advisor in the call chain
 * and before the {@link TransactionAttributesAspect} if used simultaneously.
 * See {@link org.springframework.transaction.annotation.EnableTransactionManagement} for
 * controlling weaving order.
 */
@Aspect
@Order(TransactionRetryAspect.PRECEDENCE)
public class TransactionRetryAspect {
    /**
     * The precedence at which this advice is ordered by which also controls
     * the order it is invoked in the call chain between a source and target.
     */
    public static final int PRECEDENCE = AdvisorOrder.TRANSACTION_RETRY_ADVISOR;

    static <A extends Annotation> A findAnnotation(ProceedingJoinPoint pjp, Class<A> annotationType) {
        return AnnotationUtils.findAnnotation(pjp.getSignature().getDeclaringType(), annotationType);
    }

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected Consumer<RetryEvent> retryEventConsumer = retryEvent -> {
    };

    public void setRetryEventConsumer(Consumer<RetryEvent> retryEventConsumer) {
        this.retryEventConsumer = retryEventConsumer;
    }

    @Around(value = "org.springframework.data.cockroachdb.aspect.Pointcuts.anyRetryableOperation(retryable)", argNames = "pjp,retryable")
    public Object doRetryableOperation(ProceedingJoinPoint pjp, Retryable retryable) throws Throwable {
        Assert.isTrue(!TransactionSynchronizationManager.isActualTransactionActive(),
                "Expecting NO active transaction - check advice @Order and @EnableTransactionManagement order");

        // Grab from type if needed (for non-annotated methods)
        if (retryable == null) {
            retryable = AnnotationUtils.findAnnotation(pjp.getSignature().getDeclaringType(), Retryable.class);
        }

        Assert.notNull(retryable, "No @Retryable annotation found!?");

        int numCalls = 0;

        final String methodName = pjp.getSignature().toShortString();
        final List<SQLException> sqlExceptions = new ArrayList<>();
        final Instant callTime = Instant.now();

        do {
            final Throwable throwable;
            try {
                numCalls++;

                Object rv = pjp.proceed(); // coin toss

                if (numCalls > 1) {
                    handleRecovery(sqlExceptions, numCalls, methodName, Duration.between(callTime, Instant.now()));
                }

                return rv;
            } catch (UndeclaredThrowableException ex) {
                throwable = ex.getUndeclaredThrowable();
            } catch (Exception ex) { // Catch r/w and commit time exceptions
                throwable = ex;
            }

            Throwable cause = NestedExceptionUtils.getMostSpecificCause(throwable);
            if (cause instanceof SQLException) {
                SQLException sqlException = (SQLException) cause;
                if (isRetryable(sqlException)) {
                    sqlExceptions.add(sqlException);
                    handleTransientException(sqlException, numCalls, methodName, retryable.maxBackoff());
                } else {
                    handleNonTransientException(sqlException);
                    throw throwable;
                }
            } else {
                throw throwable;
            }
        } while (numCalls < retryable.retryAttempts());

        throw new ConcurrencyFailureException(
                "Too many serialization errors (" + numCalls + ") for method [" + pjp.getSignature().toShortString()
                        + "]. Giving up!");
    }

    protected boolean isRetryable(SQLException sqlException) {
        // 40001 is the only state code we are looking for in terms of safe retries
        return PSQLState.SERIALIZATION_FAILURE.getState().equals(sqlException.getSQLState());
    }

    protected void handleRecovery(List<SQLException> sqlExceptions, int numCalls, String methodName,
                                  Duration elapsedTime) {
        String message = "Recovered from transient SQL error after "
                + numCalls + " calls to '"
                + methodName + "' time spent: ("
                + elapsedTime.toString() + ")";
        logger.info(message);

        retryEventConsumer.accept(new RetryEvent(this, message, sqlExceptions));
    }

    protected void handleTransientException(SQLException sqlException, int numCalls, String methodName,
                                            long maxBackoff) {
        try {
            long backoffMillis = Math.min((long) (Math.pow(2, numCalls) + Math.random() * 1000), maxBackoff);
            if (numCalls <= 1 && logger.isWarnEnabled()) {
                logger.warn("Transient SQL error ({}) in call #{} to '{}' (backoff for {} ms before retry): {}",
                        sqlException.getSQLState(), numCalls, methodName, backoffMillis, sqlException.getMessage());
            }
            Thread.sleep(backoffMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void handleNonTransientException(SQLException sqlException) {
        sqlException.forEach(ex -> {
            SQLException nested = (SQLException) ex;
            logger.warn("Non-transient SQL error ({}): {}",
                    nested.getSQLState(), nested.getMessage());
        });
    }
}

