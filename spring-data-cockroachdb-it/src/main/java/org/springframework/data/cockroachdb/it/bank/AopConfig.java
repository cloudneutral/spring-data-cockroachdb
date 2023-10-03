package org.springframework.data.cockroachdb.it.bank;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;
import org.springframework.data.cockroachdb.aspect.TransactionAttributesAspect;
import org.springframework.data.cockroachdb.aspect.TransactionRetryAspect;
import org.springframework.data.cockroachdb.it.TestProfiles;
import org.springframework.data.cockroachdb.it.bank.service.OutboxAspect;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AopConfig {
    @Bean
    @Profile(TestProfiles.APP_RETRY)
    public TransactionRetryAspect transactionRetryAspect() {
        return new TransactionRetryAspect();
    }

    @Bean
    @Profile(TestProfiles.APP_RETRY)
    public TransactionAttributesAspect transactionAttributesAspect(JdbcTemplate jdbcTemplate) {
        return new TransactionAttributesAspect(jdbcTemplate);
    }

    @Bean
    public OutboxAspect outboxAspect() {
        return new OutboxAspect();
    }

}
