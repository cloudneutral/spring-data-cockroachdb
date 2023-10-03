package org.springframework.data.cockroachdb.it.bank.service;

import java.io.StringReader;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.cockroachdb.it.bank.model.Transaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class OutboxAspect {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final ObjectMapper mapper = new ObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @AfterReturning(pointcut = "execution(* org.springframework.data.cockroachdb.it.bank.service.TransactionServiceImpl.submitTransferRequest(..))",
            returning = "transactionEntity")
    public void doAfterSubmitTransfer(Transaction transactionEntity) {
        Assert.isTrue(TransactionSynchronizationManager.isActualTransactionActive(),
                "Expecting active transaction - check advice @Order");

        try {
            String payload;
            if (logger.isTraceEnabled()) {
                payload = mapper.writer()
                        .withDefaultPrettyPrinter()
                        .writeValueAsString(transactionEntity);
                logger.trace("Writing payload to outbox: {}", payload);
            } else {
                payload = mapper.writer()
                        .writeValueAsString(transactionEntity);
            }

            jdbcTemplate.update(
                    "INSERT INTO outbox (aggregate_type,aggregate_id,event_type,payload) VALUES (?,?,?,?)",
                    ps -> {
                        ps.setString(1, "Transaction");
                        ps.setString(2, transactionEntity.getId().toString());
                        ps.setString(3, "TransactionCreatedEvent");
                        ps.setCharacterStream(4, new StringReader(payload));
                    });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error serializing outbox JSON payload", e);
        }
    }
}

