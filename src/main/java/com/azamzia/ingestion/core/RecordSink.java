package com.azamzia.ingestion.core;

import java.util.List;

/** Destination for accepted records. A batch must be written atomically (all or nothing). */
public interface RecordSink {
    void writeBatch(String fileKey, List<TransactionRecord> batch);
}
