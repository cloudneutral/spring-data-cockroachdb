package org.springframework.data.cockroachdb.it.bank.repository;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.cockroachdb.annotations.NotTransactional;
import org.springframework.data.cockroachdb.it.TestProfiles;
import org.springframework.data.cockroachdb.it.bank.model.Account;
import org.springframework.data.cockroachdb.it.bank.model.AccountSummary;
import org.springframework.data.cockroachdb.it.bank.model.AccountType;
import org.springframework.data.cockroachdb.it.bank.model.ForeignSystem;
import org.springframework.data.cockroachdb.it.bank.model.Money;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Profile(TestProfiles.JDBC)
@Repository
public class JdbcAccountRepositoryImpl implements AccountRepository {
    private final ObjectMapper mapper = new ObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public Integer nextSequenceNumber() {
        return jdbcTemplate.queryForObject("select nextval('account_name_sequence')", Integer.class);
    }

    @Override
    @NotTransactional // Use implicit
    public void createAccounts(int numAccounts, int batchSize, Supplier<Account> accountSupplier) {
        final List<Integer> batchSequence = new ArrayList<>();

        final Supplier<Integer> sequenceIds = () -> {
            if (batchSequence.isEmpty()) {
                int nextNo = nextSequenceNumber();
                IntStream.rangeClosed(nextNo, nextNo - 1 + 64).forEach(batchSequence::add);
            }
            return batchSequence.remove(0);
        };

        for (int i = 0; i < numAccounts; i += batchSize) {
            if (i + batchSize > numAccounts) {
                batchSize = numAccounts - i;
            }

            final int currentBatch = batchSize;

            jdbcTemplate.batchUpdate(
                    "INSERT INTO account "
                            + "(region, balance, currency, name, description, account_type, closed, allow_negative, metadata) "
                            + "VALUES(?,?,?,?,?,?,?,?,?)",
                    new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            Account account = accountSupplier.get();

                            ps.setString(1, account.getRegion());
                            ps.setBigDecimal(2, account.getBalance().getAmount());
                            ps.setString(3, account.getBalance().getCurrency().getCurrencyCode());
                            ps.setString(4, "user:" + sequenceIds.get());
                            ps.setString(5, account.getDescription());
                            ps.setString(6, account.getAccountType().getCode());
                            ps.setBoolean(7, account.isClosed());
                            ps.setInt(8, account.getAllowNegative());

                            try {
                                ps.setObject(9, mapper.writer()
                                        .writeValueAsString(account.getForeignSystem()));
                            } catch (JsonProcessingException e) {
                                throw new SQLException(e);
                            }
                        }

                        @Override
                        public int getBatchSize() {
                            return currentBatch;
                        }
                    });
        }
    }

    @Override
    public Money getBalance(Long id) {
        return this.jdbcTemplate.queryForObject(
                "SELECT balance,currency "
                        + "FROM account a "
                        + "WHERE id=?",
                (rs, rowNum) -> Money.of(rs.getString(1), rs.getString(2)),
                id
        );
    }

    @Override
    public Money getBalanceSnapshot(Long id) {
        return this.jdbcTemplate.queryForObject(
                "SELECT balance,currency "
                        + "FROM account a AS OF SYSTEM TIME follower_read_timestamp() "
                        + "WHERE id=?",
                (rs, rowNum) -> Money.of(rs.getString(1), rs.getString(2)),
                id
        );
    }

    @Override
    public List<Money> getTotalBalance() {
        return namedParameterJdbcTemplate.query(
                "SELECT sum(balance) tot_balance, "
                        + "currency "
                        + "FROM account "
                        + "GROUP BY currency",
                (rs, rowNum) -> {
                    Currency c = Currency.getInstance(rs.getString(2));
                    return Money.of(rs.getBigDecimal(1), c);
                });
    }

    @Override
    public void updateBalances(List<Account> accounts) {
        int[] rowsAffected = jdbcTemplate.batchUpdate(
                "UPDATE account "
                        + "SET "
                        + "   balance = ?,"
                        + "   updated_at=clock_timestamp() "
                        + "WHERE id = ? "
                        + "   AND closed=false "
                        + "   AND currency=? "
                        + "   AND (?) * abs(allow_negative-1) >= 0",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Account account = accounts.get(i);

                        ps.setBigDecimal(1, account.getBalance().getAmount());
                        ps.setObject(2, account.getId());
                        ps.setString(3, account.getBalance().getCurrency().getCurrencyCode());
                        ps.setBigDecimal(4, account.getBalance().getAmount());
                    }

                    @Override
                    public int getBatchSize() {
                        return accounts.size();
                    }
                });

        // Trust but verify
        Arrays.stream(rowsAffected).filter(i -> i != 1).forEach(i -> {
            throw new IncorrectResultSizeDataAccessException(1, i);
        });
    }

    @Override
    public List<Account> findByIDs(Set<Long> ids) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("ids", new HashSet<>(ids));

        return this.namedParameterJdbcTemplate.query(
                "SELECT * FROM account WHERE id in (:ids)",
                parameters,
                (rs, rowNum) -> readAccount(rs));
    }

    @Override
    public List<Account> findByRegion(String region, int offset, int limit) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("region", region);
        parameters.addValue("offset", offset);
        parameters.addValue("limit", limit);

        return this.namedParameterJdbcTemplate.query(
                "SELECT * FROM account WHERE region=:region "
                        + "OFFSET (:offset) LIMIT (:limit)",
                parameters, (rs, rowNum) -> readAccount(rs));
    }

    @Override
    public AccountSummary reportSummary(String region) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("region", region);

        return namedParameterJdbcTemplate.queryForObject(
                "SELECT "
                        + "count(a.id) tot_accounts, "
                        + "sum(a.balance) tot_balance, "
                        + "min(a.balance) min_balance, "
                        + "max(a.balance) max_balance, "
                        + "avg(a.balance) avg_balance, "
                        + "a.currency "
                        + "FROM account a "
                        + "WHERE a.region=:region "
                        + "GROUP BY a.region, a.currency",
                parameters,
                (rs, rowNum) -> {
                    AccountSummary summary = new AccountSummary();
                    summary.setRegion(region);
                    summary.setNumberOfAccounts(rs.getInt(1));
                    summary.setTotalBalance(rs.getBigDecimal(2));
                    summary.setMinBalance(rs.getBigDecimal(3));
                    summary.setMaxBalance(rs.getBigDecimal(4));
                    summary.setAvgBalance(rs.getBigDecimal(5));
                    summary.setCurrency(Currency.getInstance(rs.getString(6)));
                    return summary;
                });
    }

    private Account readAccount(ResultSet rs) throws SQLException {
        ForeignSystem foreignSystem;

        try {
            foreignSystem = mapper.readValue(rs.getCharacterStream("metadata"), ForeignSystem.class);
        } catch (IOException e) {
            throw new SQLException(e);
        }

        Timestamp ts = rs.getTimestamp("updated_at");

        return Account.builder()
                .withId(rs.getLong("id"))
                .withRegion(rs.getString("region"))
                .withName(rs.getString("name"))
                .withBalance(Money.of(rs.getString("balance"), rs.getString("currency")))
                .withAccountType(AccountType.of(rs.getString("account_type")))
                .withDescription(rs.getString("description"))
                .withClosed(rs.getBoolean("closed"))
                .withAllowNegative(rs.getInt("allow_negative") > 0)
                .withInsertedAt(rs.getTimestamp("inserted_at").toLocalDateTime())
                .withUpdatedAt(ts != null ? ts.toLocalDateTime() : null)
                .withForeignSystem(foreignSystem)
                .build();
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("delete from account where 1=1");
    }
}
