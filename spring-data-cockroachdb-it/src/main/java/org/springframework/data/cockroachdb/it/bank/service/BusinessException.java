package org.springframework.data.cockroachdb.it.bank.service;

/**
 * Base type for unrecoverable business exceptions.
 */
public abstract class BusinessException extends RuntimeException {
    protected BusinessException(String message) {
        super(message);
    }
}
