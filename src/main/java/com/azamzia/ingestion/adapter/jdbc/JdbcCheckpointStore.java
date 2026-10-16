package com.azamzia.ingestion.adapter.jdbc;

import com.azamzia.ingestion.core.CheckpointStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.OptionalLong;

@Component
public class JdbcCheckpointStore implements CheckpointStore {

    private final JdbcTemplate jdbc;

    public JdbcCheckpointStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public OptionalLong lastCommittedLine(String fileKey) {
        List<Long> rows = jdbc.queryForList(
                "SELECT last_line FROM ingestion_checkpoint WHERE file_key = ?", Long.class, fileKey);
        return rows.isEmpty() ? OptionalLong.empty() : OptionalLong.of(rows.get(0));
    }

    @Override
    public void commit(String fileKey, long lineNumber) {
        jdbc.update("""
                INSERT INTO ingestion_checkpoint (file_key, last_line, updated_at) VALUES (?, ?, now())
                ON CONFLICT (file_key) DO UPDATE SET last_line = EXCLUDED.last_line, updated_at = now()
                """, fileKey, lineNumber);
    }
}
