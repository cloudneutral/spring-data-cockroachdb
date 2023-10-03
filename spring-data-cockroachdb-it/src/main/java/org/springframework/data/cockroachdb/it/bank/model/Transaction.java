package org.springframework.data.cockroachdb.it.bank.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.*;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

/**
 * Represents a monetary transaction (balance update) between at least two different accounts.
 * <p>
 * JPA annotations are only used by JPA server implementation.
 */
@Entity
@Table(name = "transaction")
@DynamicInsert
@DynamicUpdate
public class Transaction extends AbstractEntity<Long> {
    @Id
    @Column(updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false)
    private UUID token;

    @Column(updatable = false)
    private String region;

    @Column(name = "transaction_type")
    private String transactionType;

    @Column(name = "transfer_date", nullable = false, updatable = false)
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate transferDate;

    @Column(name = "booking_date", nullable = false, updatable = false)
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate bookingDate;

    @OneToMany(orphanRemoval = true, mappedBy = "id.transaction", fetch = FetchType.LAZY,
            cascade = {CascadeType.MERGE, CascadeType.REFRESH, CascadeType.REMOVE})
    private List<TransactionItem> items;

    public Transaction() {
        this.items = new ArrayList<>();
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getToken() {
        return token;
    }

    public void setToken(UUID token) {
        this.token = token;
    }

    public String getRegion() {
        return region;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public LocalDate getTransferDate() {
        return transferDate;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setTransferDate(LocalDate transferDate) {
        this.transferDate = transferDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public List<TransactionItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Transaction that = (Transaction) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Transaction instance = new Transaction();

        public Builder withId(Long id) {
            instance.id = id;
            return this;
        }

        public Builder withToken(UUID token) {
            instance.token = token;
            return this;
        }

        public Builder withRegion(String region) {
            instance.region = region;
            return this;
        }

        public Builder withTransactionType(String transactionType) {
            instance.transactionType = transactionType;
            return this;
        }

        public Builder withBookingDate(LocalDate bookingDate) {
            instance.bookingDate = bookingDate;
            return this;
        }

        public Builder withTransferDate(LocalDate transferDate) {
            instance.transferDate = transferDate;
            return this;
        }

        public TransactionItem.Builder andItem() {
            return TransactionItem.builder(this, instance.items::add);
        }

        public Transaction build() {
            return instance;
        }
    }
}
