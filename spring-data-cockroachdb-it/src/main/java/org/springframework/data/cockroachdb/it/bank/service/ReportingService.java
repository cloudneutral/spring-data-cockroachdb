package org.springframework.data.cockroachdb.it.bank.service;

import org.springframework.data.cockroachdb.it.bank.model.AccountSummary;

public interface ReportingService {
    AccountSummary accountSummary(String region);
}
