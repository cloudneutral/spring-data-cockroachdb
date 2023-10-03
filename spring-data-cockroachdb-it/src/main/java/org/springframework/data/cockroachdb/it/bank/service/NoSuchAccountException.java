package org.springframework.data.cockroachdb.it.bank.service;

public class NoSuchAccountException extends BusinessException {
    public NoSuchAccountException(String name) {
        super("No such account: " + name);
    }
}
