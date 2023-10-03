package org.springframework.data.cockroachdb.it.bank.model;

import java.util.EnumSet;

public enum AccountType {
    EXPENSE("E"), ASSET("A"), REVENUE("R"), LIABILITY("L");

    private final String code;

    AccountType(String code) {
        this.code = code;
    }

    public static AccountType of(String code) {
        for (AccountType accountType : EnumSet.allOf(AccountType.class)) {
            if (accountType.code.equals(code)) {
                return accountType;
            }
        }
        throw new IllegalArgumentException("No such type: " + code);
    }

    public String getCode() {
        return code;
    }
}
