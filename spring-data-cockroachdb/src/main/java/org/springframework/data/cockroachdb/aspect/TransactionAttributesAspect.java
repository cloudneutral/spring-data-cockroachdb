package org.springframework.data.cockroachdb.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.cockroachdb.annotations.SetVariable;
import org.springframework.data.cockroachdb.annotations.TimeTravel;
import org.springframework.data.cockroachdb.annotations.TransactionBoundary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * AOP aspect that sets specific and arbitrary transaction/session variables.
 * <p>
 * The main pre-condition is that there must be an existing transaction in scope.
 * This advice must be applied after the {@link TransactionRetryAspect} if used simultaneously,
 * and the Spring transaction advisor in the call chain.
 * <p>
 * See {@link org.springframework.transaction.annotation.EnableTransactionManagement} for
 * controlling weaving order.
 */
@Aspect
@Order(TransactionAttributesAspect.PRECEDENCE)
public class TransactionAttributesAspect {
    /**
     * The precedence at which this advice is ordered by which also controls
     * the order it is invoked in the call chain between a source and target.
     */
    public static final int PRECEDENCE = AdvisorOrder.TRANSACTION_ATTRIBUTES_ADVISOR;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final JdbcTemplate jdbcTemplate;

    public TransactionAttributesAspect(@Autowired JdbcTemplate jdbcTemplate) {
        Assert.notNull(jdbcTemplate, "jdbcTemplate is null");
        this.jdbcTemplate = jdbcTemplate;
    }

    @Around(value = "org.springframework.data.cockroachdb.aspect.Pointcuts.anyTransactionBoundaryOperation(transactionBoundary)",
            argNames = "pjp,transactionBoundary")
    public Object doInTransaction(ProceedingJoinPoint pjp, TransactionBoundary transactionBoundary)
            throws Throwable {
        Assert.isTrue(TransactionSynchronizationManager.isActualTransactionActive(),
                "Expecting active transaction - check advice @Order and @EnableTransactionManagement order");

        // Grab from type if needed (for non-annotated methods)
        if (transactionBoundary == null) {
            transactionBoundary = TransactionRetryAspect.findAnnotation(pjp, TransactionBoundary.class);
        }

        Assert.notNull(transactionBoundary, "No @TransactionBoundary annotation found!?");

        if (!"(empty)".equals(transactionBoundary.applicationName())) {
            jdbcTemplate.update("SET application_name=?", transactionBoundary.applicationName());
        }

        if (!TransactionBoundary.Priority.normal.equals(transactionBoundary.priority())) {
            jdbcTemplate.execute("SET TRANSACTION PRIORITY " + transactionBoundary.priority().name());
        }

        if (!"0s".equals(transactionBoundary.idleTimeout())) {
            jdbcTemplate.update("SET idle_in_transaction_session_timeout=?", transactionBoundary.idleTimeout());
        }

        if (transactionBoundary.readOnly()) {
            jdbcTemplate.execute("SET transaction_read_only=true");
        }

        TimeTravel timeTravel = transactionBoundary.timeTravel();

        if (timeTravel.mode().equals(TimeTravelMode.FOLLOWER_READ)) {
            jdbcTemplate.execute("SET TRANSACTION AS OF SYSTEM TIME follower_read_timestamp()");
        } else if (timeTravel.mode().equals(TimeTravelMode.HISTORICAL_READ)) {
            jdbcTemplate.update("SET TRANSACTION AS OF SYSTEM TIME INTERVAL '"
                    + timeTravel.interval() + "'");
        }

        for (SetVariable var : transactionBoundary.variables()) {
            if (!var.variable().isMutable()) {
                throw new InvalidDataAccessApiUsageException(
                        "Attempting to modify a read-only session variable: " + var.variable().name());
            }
            if (var.intValue() >= 0) {
                jdbcTemplate.update("SET " + var.scope().name() + " " + var.variable() + " = " + var.intValue());
            } else {
                jdbcTemplate.update("SET " + var.scope().name() + " " + var.variable() + " = ?", var.value());
            }
        }

        return pjp.proceed();
    }
}
