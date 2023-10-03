package org.springframework.data.cockroachdb.it.bank.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.data.cockroachdb.it.TestProfiles;
import org.springframework.data.cockroachdb.it.bank.model.Transaction;
import org.springframework.data.cockroachdb.it.bank.model.TransactionItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Profile(TestProfiles.JDBC)
@Repository
public class JdbcTransactionRepositoryImpl implements TransactionRepository {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Optional<Transaction> findTransactionByToken(UUID token) {
        List<Transaction> list = this.jdbcTemplate.query(
                "SELECT * FROM transaction WHERE token=?",
                (rs, rowNum) -> mapToTransaction(rs),
                token);
        return Optional.ofNullable(DataAccessUtils.singleResult(list));
    }

    @Override
    public Transaction createTransaction(Transaction transaction) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection
                    .prepareStatement("INSERT INTO transaction "
                        + "(token,region,booking_date,transfer_date,transaction_type) "
                        + "VALUES(?, ?, ?, ?, ?) RETURNING id,booking_date,transfer_date", Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, transaction.getToken());
            ps.setString(2, transaction.getRegion());
            ps.setObject(3, transaction.getBookingDate());
            ps.setObject(4, transaction.getTransferDate());
            ps.setString(5, transaction.getTransactionType());
            return ps;
        }, keyHolder);

        Map<String, Object> keys =  keyHolder.getKeys();
        Long transactionId = (Long) keys.get("id");
        transaction.setId(transactionId);

        LocalDate bookingDate = ((Date)keys.get("booking_date")).toLocalDate();
        LocalDate transferDate = ((Date)keys.get("transfer_date")).toLocalDate();

        transaction.setBookingDate(bookingDate);
        transaction.setTransferDate(transferDate);

        final List<TransactionItem> items = transaction.getItems();

        jdbcTemplate.batchUpdate(
                "INSERT INTO transaction_item "
                        + "(region, transaction_id, account_id, amount, currency, note, running_balance) "
                        + "VALUES(?,?,?,?,?,?,?)", new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        TransactionItem transactionLeg = items.get(i);
                        ps.setString(1, transactionLeg.getRegion());
                        ps.setObject(2, transactionId);
                        ps.setObject(3, transactionLeg.getAccount().getId());
                        ps.setBigDecimal(4, transactionLeg.getAmount().getAmount());
                        ps.setString(5, transactionLeg.getAmount().getCurrency().getCurrencyCode());
                        ps.setString(6, transactionLeg.getNote());
                        ps.setBigDecimal(7, transactionLeg.getRunningBalance().getAmount());
                    }

                    @Override
                    public int getBatchSize() {
                        return items.size();
                    }
                });

        return transaction;
    }

    @Override
    public Page<Transaction> findTransactions(Pageable pageable) {
        int count = countAllTransactions();
        List<Transaction> content = this.jdbcTemplate.query(
                "SELECT * FROM transaction ORDER BY transfer_date LIMIT ? OFFSET ?",
                (rs, rowNum) -> mapToTransaction(rs),
                pageable.getPageSize(), pageable.getOffset());
        return new PageImpl<>(content, pageable, count);
    }

    private Transaction mapToTransaction(ResultSet rs) throws SQLException {
        Long transactionId = rs.getLong("id");
        UUID token = (UUID) rs.getObject("token");
        String region = rs.getString("region");
        String transactionType = rs.getString("transaction_type");
        LocalDate bookingDate = rs.getDate("booking_date").toLocalDate();
        LocalDate transferDate = rs.getDate("transfer_date").toLocalDate();
        return Transaction.builder()
                .withId(transactionId)
                .withToken(token)
                .withRegion(region)
                .withTransactionType(transactionType)
                .withBookingDate(bookingDate)
                .withTransferDate(transferDate)
                .build();
    }

    private Integer countAllTransactions() {
        return jdbcTemplate.queryForObject("SELECT count(id) FROM transaction", Integer.class);
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("delete from transaction_item where 1=1");
        jdbcTemplate.update("delete from transaction where 1=1");
    }
}
