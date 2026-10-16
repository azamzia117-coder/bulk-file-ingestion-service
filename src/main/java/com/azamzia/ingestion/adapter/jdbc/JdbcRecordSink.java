package com.azamzia.ingestion.adapter.jdbc;

import com.azamzia.ingestion.core.RecordSink;
import com.azamzia.ingestion.core.TransactionRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Date;
import java.util.List;

/**
 * Writes a batch in a single transaction. {@code ON CONFLICT DO NOTHING} on the primary key is a second
 * line of defence against duplicates if a checkpoint is ever lost.
 */
@Component
public class JdbcRecordSink implements RecordSink {

    private static final String SQL = """
            INSERT INTO transaction_record (record_id, account_id, amount_minor, currency, posted_date, source_file)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (record_id) DO NOTHING
            """;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public JdbcRecordSink(JdbcTemplate jdbc, TransactionTemplate tx) {
        this.jdbc = jdbc;
        this.tx = tx;
    }

    @Override
    public void writeBatch(String fileKey, List<TransactionRecord> batch) {
        tx.executeWithoutResult(status -> jdbc.batchUpdate(SQL, batch, batch.size(), (ps, r) -> {
            ps.setString(1, r.recordId());
            ps.setString(2, r.accountId());
            ps.setLong(3, r.amountMinor());
            ps.setString(4, r.currency());
            ps.setDate(5, Date.valueOf(r.postedDate()));
            ps.setString(6, fileKey);
        }));
    }
}
