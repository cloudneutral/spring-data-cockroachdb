package org.springframework.data.cockroachdb.it.bank.model;

import java.math.BigDecimal;
import java.util.Currency;

public class AccountSummary {
    private String region;

    private Currency currency;

    private int numberOfAccounts;

    private BigDecimal totalBalance;

    private BigDecimal minBalance;

    private BigDecimal maxBalance;

    private BigDecimal avgBalance;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public int getNumberOfAccounts() {
        return numberOfAccounts;
    }

    public AccountSummary setNumberOfAccounts(int numberOfAccounts) {
        this.numberOfAccounts = numberOfAccounts;
        return this;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public AccountSummary setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
        return this;
    }

    public BigDecimal getMinBalance() {
        return minBalance;
    }

    public AccountSummary setMinBalance(BigDecimal minBalance) {
        this.minBalance = minBalance;
        return this;
    }

    public BigDecimal getMaxBalance() {
        return maxBalance;
    }

    public AccountSummary setMaxBalance(BigDecimal maxBalance) {
        this.maxBalance = maxBalance;
        return this;
    }

    public BigDecimal getAvgBalance() {
        return avgBalance;
    }

    public AccountSummary setAvgBalance(BigDecimal avgBalance) {
        this.avgBalance = avgBalance;
        return this;
    }

    @Override
    public String toString() {
        return "AccountSummary{" +
                "region='" + region + '\'' +
                ", currency=" + currency +
                ", numberOfAccounts=" + numberOfAccounts +
                ", totalBalance=" + totalBalance +
                ", minBalance=" + minBalance +
                ", maxBalance=" + maxBalance +
                ", avgBalance=" + avgBalance +
                '}';
    }
}

