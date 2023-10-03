package org.springframework.data.cockroachdb.it.bank.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Consumer;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Immutable transaction item/leg representing a single account balance update as part
 * of a balanced, multi-legged monetary transaction. Mapped as join with attributes
 * between account and transaction entities.
 * <p>
 * JPA annotations are only used by JPA server implementation.
 */
@Entity
@Table(name = "transaction_item")
public class TransactionItem extends AbstractEntity<TransactionItem.Id> {
    @EmbeddedId
    private Id id = new Id();

    @Column(name = "region", updatable = false)
    private String region;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false, updatable = false)),
            @AttributeOverride(name = "currency", column = @Column(name = "currency", length = 3, nullable = false, updatable = false))})
    private Money amount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "running_balance", nullable = false, updatable = false)),
            @AttributeOverride(name = "currency", column = @Column(name = "currency", length = 3, nullable = false, insertable = false, updatable = false))})
    private Money runningBalance;

    @Column(name = "note", length = 128, updatable = false)
    @Basic(fetch = FetchType.LAZY)
    private String note;

    protected TransactionItem() {
    }

    public static Builder builder(Transaction.Builder parentBuilder, Consumer<TransactionItem> callback) {
        return new Builder(parentBuilder, callback);
    }

    @Override
    public Id getId() {
        return id;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Money getAmount() {
        return amount;
    }

    public void setAmount(Money amount) {
        this.amount = amount;
    }

    public Money getRunningBalance() {
        return runningBalance;
    }

    public void setRunningBalance(Money runningBalance) {
        this.runningBalance = runningBalance;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @JsonIgnore
    public Account getAccount() {
        return id.getAccount();
    }

    public void setAccount(Account account) {
        this.id.account = account;
    }

    @JsonIgnore
    public Transaction getTransaction() {
        return id.getTransaction();
    }

    public void setTransaction(Transaction transaction) {
        this.id.transaction = transaction;
    }

    @Embeddable
    public static class Id implements Serializable {
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "account_id", referencedColumnName = "id", nullable = false)
        @JsonIgnore // prevents inf recursion in outbox json
        private Account account;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "transaction_id", referencedColumnName = "id", nullable = false)
        @JsonIgnore // prevents inf recursion in outbox json
        private Transaction transaction;

        protected Id() {
        }

        public Account getAccount() {
            return account;
        }

        public Transaction getTransaction() {
            return transaction;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Id id = (Id) o;
            return account.equals(id.account) && transaction.equals(id.transaction);
        }

        @Override
        public int hashCode() {
            return Objects.hash(account, transaction);
        }
    }

    public static class Builder {
        private final Transaction.Builder parentBuilder;

        private final Consumer<TransactionItem> callback;

        private Money amount;

        private Money runningBalance;

        private Account account;

        private String note;

        private String region;

        private Builder(Transaction.Builder parentBuilder, Consumer<TransactionItem> callback) {
            this.parentBuilder = parentBuilder;
            this.callback = callback;
        }

        public Builder withAmount(Money amount) {
            this.amount = amount;
            return this;
        }

        public Builder withRunningBalance(Money runningBalance) {
            this.runningBalance = runningBalance;
            return this;
        }

        public Builder withAccount(Account account) {
            this.account = account;
            return this;
        }

        public Builder withNote(String note) {
            this.note = note;
            return this;
        }

        public Builder withRegion(String region) {
            this.region = region;
            return this;
        }

        public Transaction.Builder then() {
            Assert.notNull(account, "account is null");

            TransactionItem transactionItem = new TransactionItem();
            transactionItem.setAccount(account);
            transactionItem.setAmount(amount);
            transactionItem.setRunningBalance(runningBalance);
            transactionItem.setNote(note);
            transactionItem.setRegion(region);

            callback.accept(transactionItem);

            return parentBuilder;
        }
    }
}
