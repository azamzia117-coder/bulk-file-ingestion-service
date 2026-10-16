package com.azamzia.ingestion.core;

import java.util.List;

/**
 * Outcome of one run. {@code rowsRead == accepted + rejected} always holds for the lines processed in this
 * run, which is the property that guarantees no row is silently dropped.
 */
public record IngestionReport(String fileKey, long resumedAfterLine, long rowsRead, long accepted,
                              long rejected, int batchesCommitted, List<RejectedRow> rejectedRows) {

    public boolean balanced() {
        return rowsRead == accepted + rejected;
    }
}
