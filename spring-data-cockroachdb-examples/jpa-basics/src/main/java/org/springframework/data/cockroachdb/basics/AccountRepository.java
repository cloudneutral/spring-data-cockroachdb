package org.springframework.data.cockroachdb.basics;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByName(String name);

    @Query(value = "select a.balance "
            + "from Account a "
            + "where a.id = ?1")
    BigDecimal findBalanceById(Long id);

    @Query(value = "select a.balance "
            + "from account a AS OF SYSTEM TIME follower_read_timestamp() "
            + "where a.id = ?1", nativeQuery = true)
    BigDecimal findBalanceSnapshotById(Long id);
}
