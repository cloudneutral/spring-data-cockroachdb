package org.springframework.data.cockroachdb.basics;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Entity representing a monetary account like an asset, liability, expense or capital account.
 */
@Table(name = "account")
public class Account extends AbstractEntity<Long> {
    @Id
    @Column
    private Long id;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String accountType;

    @Column
    private LocalDateTime insertedAt;

    @Column
    private LocalDateTime updatedAt;

    @Column
    private BigDecimal balance;

    @Column
    private String currency;

    @Column
    private boolean closed;

    @Column
    private int allowNegative;

    protected Account() {
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType.getCode();
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void setAllowNegative(int allowNegative) {
        this.allowNegative = allowNegative;
    }

    public BigDecimal addAmount(BigDecimal amount) {
        this.balance = balance.add(amount);
        return this.balance;
    }

    public AccountType getAccountType() {
        return AccountType.of(accountType);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public LocalDateTime getInsertedAt() {
        return insertedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public int getAllowNegative() {
        return allowNegative;
    }

    public String toDisplayString() {
        return toString();
    }

    public void setInsertedAt(LocalDateTime insertedAt) {
        this.insertedAt = insertedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Account)) {
            return false;
        }

        Account that = (Account) o;

        if (!id.equals(that.id)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static final class Builder {
        private final Account instance = new Account();

        public Builder withId(Long accountId) {
            this.instance.id = accountId;
            return this;
        }

        public Builder withName(String name) {
            this.instance.name = name;
            return this;
        }

        public Builder withBalance(BigDecimal balance) {
            this.instance.balance = balance;
            return this;
        }

        public Builder withCurrency(String currency) {
            this.instance.currency = currency;
            return this;
        }

        public Builder withAccountType(AccountType accountType) {
            this.instance.accountType = accountType.getCode();
            return this;
        }

        public Builder withClosed(boolean closed) {
            this.instance.closed = closed;
            return this;
        }

        public Builder withAllowNegative(boolean allowNegative) {
            this.instance.allowNegative = allowNegative ? 1 : 0;
            return this;
        }

        public Builder withDescription(String description) {
            this.instance.description = description;
            return this;
        }

        public Builder withInsertedAt(LocalDateTime dateTime) {
            this.instance.insertedAt = dateTime;
            return this;
        }

        public Builder withUpdatedAt(LocalDateTime dateTime) {
            this.instance.updatedAt = dateTime;
            return this;
        }

        public Account build() {
            return instance;
        }
    }
}
