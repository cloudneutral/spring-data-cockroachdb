package org.springframework.data.cockroachdb.it.bank.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cockroachdb.annotations.TimeTravel;
import org.springframework.data.cockroachdb.annotations.TransactionBoundary;
import org.springframework.data.cockroachdb.aspect.TimeTravelMode;
import org.springframework.data.cockroachdb.it.bank.model.AccountSummary;
import org.springframework.data.cockroachdb.it.bank.repository.AccountRepository;
import org.springframework.stereotype.Service;

@Service
public class ReportingServiceImpl implements ReportingService {
    @Autowired
    private AccountRepository accountRepository;

    @Override
    @TransactionBoundary(timeTravel = @TimeTravel(mode = TimeTravelMode.FOLLOWER_READ), readOnly = true)
    public AccountSummary accountSummary(String region) {
        return accountRepository.reportSummary(region);
    }
}
