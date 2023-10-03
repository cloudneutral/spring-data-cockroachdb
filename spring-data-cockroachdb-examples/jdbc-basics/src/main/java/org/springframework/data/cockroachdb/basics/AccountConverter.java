package org.springframework.data.cockroachdb.basics;

import java.time.LocalDateTime;

import org.springframework.data.relational.core.conversion.MutableAggregateChange;
import org.springframework.data.relational.core.mapping.event.AfterConvertCallback;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.data.relational.core.mapping.event.BeforeSaveCallback;
import org.springframework.stereotype.Component;

@Component
public class AccountConverter implements
        BeforeConvertCallback<Account>,
        BeforeSaveCallback<Account>,
        AfterConvertCallback<Account> {
    @Override
    public Account onBeforeConvert(Account aggregate) {
        if (aggregate.isNew()) {
//            aggregate.setId(UUID.randomUUID());
        }
        return aggregate;
    }

    @Override
    public Account onAfterConvert(Account aggregate) {
        aggregate.markNotNew();
        return aggregate;
    }

    @Override
    public Account onBeforeSave(Account aggregate, MutableAggregateChange<Account> aggregateChange) {
        if (aggregate.isNew()) {
            aggregate.setInsertedAt(LocalDateTime.now());
        }
        return aggregate;
    }
}
