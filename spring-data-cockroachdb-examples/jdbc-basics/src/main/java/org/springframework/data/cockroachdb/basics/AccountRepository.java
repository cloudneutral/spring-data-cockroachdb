package org.springframework.data.cockroachdb.basics;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends CrudRepository<Account, Long> {
    Optional<Account> findByName(String name);

    @Query(value = "select a.balance "
            + "from account a "
            + "where a.id = :id")
    BigDecimal findBalanceById(Long id);

    @Query(value = "select a.balance "
            + "from account a as of system time follower_read_timestamp() "
            + "where a.id = :id")
    BigDecimal findBalanceSnapshotById(Long id);
}
