package org.springframework.data.cockroachdb.aspect;

import java.lang.reflect.UndeclaredThrowableException;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.concurrent.atomic.AtomicLong;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.cockroachdb.annotations.Retryable;
import org.springframework.jdbc.datasource.JdbcTransactionObjectSupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * AOP advice for making transaction boundary methods rollback to savepoint
 * on transient errors (aborts due to contention).
 * <p>
 * Requires that this advice runs in a non-TX context before the
 * TX advisor.
 *
 * @see TransactionRetryAspect
 */
@Aspect
@Order(SavepointRetryAspect.PRECEDENCE)
public class SavepointRetryAspect {
    /**
     * The precedence at which this advice is ordered by which also controls
     * the order it is invoked in the call chain between a source and target.
     */
    public static final int PRECEDENCE = AdvisorOrder.TRANSACTION_RETRY_ADVISOR;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private String savepointName;

    private PlatformTransactionManager transactionManager;

    public SavepointRetryAspect(PlatformTransactionManager transactionManager, String savepointName) {
        this.transactionManager = transactionManager;
        this.savepointName = savepointName;
    }

    @Around(value = "org.springframework.data.cockroachdb.aspect.Pointcuts.anyRetryableOperation(retryable)", argNames = "pjp,retryable")
    public Object doRetryableOperation(ProceedingJoinPoint pjp, Retryable retryable) throws Throwable {
        Object rv;

        // Grab from type if needed (for non-annotated methods)
        if (retryable == null) {
            retryable = AnnotationUtils.findAnnotation(pjp.getSignature().getDeclaringType(), Retryable.class);
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("TX already active -- possible Spring profile conflict");
        }

        AtomicLong backoffMillis = new AtomicLong(150);

        for (int outerAttempts = 1; ; outerAttempts++) {
            if (outerAttempts >= retryable.retryAttempts()) {
                throw new TransactionSystemException("Too many transaction retry:s ("
                        + retryable.retryAttempts() + ") for method ["
                        + pjp.getSignature().toShortString() + "] - giving up!");
            }

            final DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
            transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transactionDefinition.setName(Thread.currentThread().getName());

            final TransactionStatus status = transactionManager.getTransaction(transactionDefinition);

            try {
                Object savepoint = createSavepoint(status);
                for (int innerAttempts = 1; ; innerAttempts++) {
                    if (innerAttempts + outerAttempts >= retryable.retryAttempts()) {
                        throw new TransactionSystemException("Too many savepoint retry:s ("
                                + retryable.retryAttempts() + ") for method ["
                                + pjp.getSignature().toShortString() + "] - giving up!");
                    }

                    try {
                        rv = pjp.proceed(); // May throw transient errors, catch in inner loop and rollback to SP
                        break;
                    } catch (TransientDataAccessException ex) {
                        handleTransientException(ex, innerAttempts + outerAttempts, retryable.retryAttempts(),
                                pjp, backoffMillis);
                        status.rollbackToSavepoint(savepoint);
                    } catch (UndeclaredThrowableException ex) {
                        Throwable t = ex.getUndeclaredThrowable();
                        if (t instanceof TransientDataAccessException) {
                            handleTransientException(t, outerAttempts,
                                    retryable.retryAttempts(), pjp, backoffMillis);
                        } else {
                            rollbackOnException(status, ex);
                            throw ex;
                        }
                    }
                }
                status.releaseSavepoint(savepoint); // May throw transient errors, catch in outer loop and rollback entire TX
            } catch (TransientDataAccessException ex) {
                handleTransientException(ex, outerAttempts, retryable.retryAttempts(), pjp, backoffMillis);
                this.transactionManager.rollback(status);
                continue;
            } catch (RuntimeException | Error ex) {
                rollbackOnException(status, ex);
                throw ex;
            } catch (Throwable ex) {
                rollbackOnException(status, ex);
                throw new UndeclaredThrowableException(ex,
                        "TransactionCallback threw undeclared checked exception");
            }

            try {
                transactionManager.commit(status);
                break;
            } catch (TransientDataAccessException | TransactionSystemException ex) {
                handleTransientException(ex, outerAttempts, retryable.retryAttempts(), pjp, backoffMillis);
            }
        }

        return rv;
    }

    private Savepoint createSavepoint(TransactionStatus status) {
        DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;
        JdbcTransactionObjectSupport sm = (JdbcTransactionObjectSupport) defStatus.getTransaction();
        try {
            if (savepointName != null) {
                return sm.getConnectionHolder().getConnection().setSavepoint(savepointName);
            }
            return sm.getConnectionHolder().getConnection().setSavepoint();
        } catch (SQLException e) {
            throw new TransactionSystemException("Could not create savePoint", e);

        }
    }

    private void handleTransientException(Throwable ex, int numAttempts, int totalAttempts,
                                          ProceedingJoinPoint pjp, AtomicLong backoffMillis) {
        if (logger.isWarnEnabled()) {
            logger.warn("Transient data access exception (" + numAttempts + " of max " + totalAttempts + ") "
                    + " (retry in " + backoffMillis + " ms) "
                    + "in method '" + pjp.getSignature().toShortString() + "': " + ex.getMessage());
        }
        if (backoffMillis.get() >= 0) {
            try {
                Thread.sleep(backoffMillis.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
            backoffMillis.set(Math.min((long) (backoffMillis.get() * 1.5), 5000));
        }
    }

    private void rollbackOnException(TransactionStatus status, Throwable ex) throws TransactionException {
        logger.debug("Initiating transaction rollback on application exception", ex);
        try {
            this.transactionManager.rollback(status);
        } catch (TransactionSystemException ex2) {
            logger.error("Application exception overridden by rollback exception", ex);
            ex2.initApplicationException(ex);
            throw ex2;
        } catch (RuntimeException ex2) {
            logger.error("Application exception overridden by rollback exception", ex);
            throw ex2;
        } catch (Error err) {
            logger.error("Application exception overridden by rollback error", ex);
            throw err;
        }
    }
}
