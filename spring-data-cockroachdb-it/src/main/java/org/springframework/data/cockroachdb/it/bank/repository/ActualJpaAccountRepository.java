package org.springframework.data.cockroachdb.it.bank.repository;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.persistence.Tuple;

import org.springframework.context.annotation.Profile;
import org.springframework.data.cockroachdb.it.TestProfiles;
import org.springframework.data.cockroachdb.it.bank.model.Account;
import org.springframework.data.cockroachdb.it.bank.model.Money;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Profile(TestProfiles.JPA)
@Repository
public interface ActualJpaAccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByName(String name);

    @Query(value = "select nextval('account_name_sequence')", nativeQuery = true)
    Integer nextSequenceNumber();

    @Query(value = "select a.balance "
            + "from Account a "
            + "where a.id = ?1")
    Money findBalanceById(Long id);

    @Query(value = "select a.balance "
            + "from account a AS OF SYSTEM TIME follower_read_timestamp() "
            + "where a.id = ?1", nativeQuery = true)
    Money findBalanceSnapshotById(Long id);

    @Query(value = "select a "
            + "from Account a "
            + "where a.id in (?1)")
    List<Account> findAllForUpdate(Set<Long> ids);

    @Query(value = "select "
            + "count (a.id), "
            + "sum (a.balance.amount), "
            + "min (a.balance.amount), "
            + "max (a.balance.amount), "
            + "avg (a.balance.amount), "
            + "a.balance.currency "
            + "from Account a "
            + "where a.region = ?1 "
            + "group by a.region,a.balance.currency")
    Stream<Tuple> accountSummary(String region);
}
