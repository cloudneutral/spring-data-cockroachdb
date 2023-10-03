package org.springframework.data.cockroachdb.it.bank.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.cockroachdb.it.bank.service.NegativeBalanceException;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import jakarta.persistence.*;

/**
 * Represents a monetary account like asset, liability, expense, capital accounts and so forth.
 * <p>
 * JPA annotations are only used by JPA server implementation.
 */
@Entity
@Table(name = "account")
@DynamicInsert
@DynamicUpdate
public class Account extends AbstractEntity<Long> {
    @Id
    @Column(updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(updatable = false)
    private String region;

    @Column
    private String name;

    @Column
    @Basic(fetch = FetchType.LAZY)
    private String description;

    @Convert(converter = AccountTypeConverter.class)
    @Column(name = "account_type", updatable = false, nullable = false)
    private AccountType accountType;

    @Column(name = "inserted_at", updatable = false)
    @Basic(fetch = FetchType.LAZY)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime insertedAt;

    @Column(name = "updated_at")
    @Basic(fetch = FetchType.LAZY)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime updatedAt;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "balance")),
            @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    })
    private Money balance;

    @Column(nullable = false)
    private boolean closed;

    @Column(name = "allow_negative", nullable = false)
    private int allowNegative;

    @Column(name = "metadata")
    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.JSON)
    private ForeignSystem foreignSystem;

    protected Account() {
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public void setBalance(Money balance) {
        this.balance = balance;
    }

    public void setAllowNegative(int allowNegative) {
        this.allowNegative = allowNegative;
    }

    public String getRegion() {
        return region;
    }

    public void addAmount(Money amount) {
        Money newBalance = getBalance().plus(amount);
        if (getAllowNegative() == 0 && newBalance.isNegative()) {
            throw new NegativeBalanceException(toDisplayString());
        }
        this.balance = newBalance;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Money getBalance() {
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

    public ForeignSystem getForeignSystem() {
        return foreignSystem;
    }

    public void setForeignSystem(ForeignSystem foreignSystem) {
        this.foreignSystem = foreignSystem;
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

        public Builder withRegion(String region) {
            this.instance.region = region;
            return this;
        }

        public Builder withName(String name) {
            this.instance.name = name;
            return this;
        }

        public Builder withBalance(Money balance) {
            this.instance.balance = balance;
            return this;
        }

        public Builder withAccountType(AccountType accountType) {
            this.instance.accountType = accountType;
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

        public Builder withForeignSystem(ForeignSystem foreignSystem) {
            this.instance.foreignSystem = foreignSystem;
            return this;
        }

        public Account build() {
            return instance;
        }
    }
}
