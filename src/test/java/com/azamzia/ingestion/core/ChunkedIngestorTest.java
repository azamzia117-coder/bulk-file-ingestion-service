package com.azamzia.ingestion.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkedIngestorTest {

    private static final String HEADER = "record_id,account_id,amount_minor,currency,posted_date";

    private CollectingSink sink;
    private CheckpointStore.InMemory checkpoints;

    @BeforeEach
    void setUp() {
        sink = new CollectingSink();
        checkpoints = new CheckpointStore.InMemory();
    }

    private static String rows(int count) {
        StringBuilder sb = new StringBuilder(HEADER).append('\n');
        for (int i = 1; i <= count; i++) {
            sb.append(String.format("R%06d,ACC-%06d,%d,USD,2026-02-%02d%n", i, 100000 + i % 50, 1000 + i, 1 + i % 28));
        }
        return sb.toString();
    }

    @Test
    void acceptsValidRowsAndKeepsCountsBalanced() {
        String csv = rows(10) + "BAD,ACC-1,abc,US,2026-13-01\n";
        ChunkedIngestor ingestor = new ChunkedIngestor(new RecordMapper(), sink, checkpoints, 4);

        IngestionReport report = ingestor.ingest("file-a.csv", new BufferedReader(new StringReader(csv)));

        assertEquals(11L, report.rowsRead());
        assertEquals(10L, report.accepted());
        assertEquals(1L, report.rejected());
        assertTrue(report.balanced());
        assertEquals(10, sink.records.size());
        RejectedRow bad = report.rejectedRows().get(0);
        assertEquals(12L, bad.lineNumber());
        assertEquals(4, bad.errors().size(), "all problems on the row are reported: " + bad.errors());
    }

    @Test
    void resumesAfterCrashWithoutLosingOrDuplicatingRows() {
        String csv = rows(25);
        FailingSink failing = new FailingSink(sink, 3); // third batch write blows up
        ChunkedIngestor first = new ChunkedIngestor(new RecordMapper(), failing, checkpoints, 5);

        assertThrows(IllegalStateException.class,
                () -> first.ingest("file-b.csv", new BufferedReader(new StringReader(csv))));
        assertEquals(10, sink.records.size());

        ChunkedIngestor second = new ChunkedIngestor(new RecordMapper(), sink, checkpoints, 5);
        IngestionReport report = second.ingest("file-b.csv", new BufferedReader(new StringReader(csv)));

        assertEquals(11L, report.resumedAfterLine());
        assertEquals(15L, report.accepted());
        assertEquals(25, sink.records.size());
        Set<String> ids = new HashSet<>();
        sink.records.forEach(r -> ids.add(r.recordId()));
        assertEquals(25, ids.size(), "no record written twice");
    }

    @Test
    void streamsLargeFileInBoundedBatches() {
        int total = 200_000;
        Reader generated = new StringReader(rows(total));
        ChunkedIngestor ingestor = new ChunkedIngestor(new RecordMapper(), sink, checkpoints, 1_000);

        IngestionReport report = ingestor.ingest("big.csv", new BufferedReader(generated));

        assertEquals((long) total, report.accepted());
        assertEquals(200, report.batchesCommitted());
        assertTrue(sink.maxBatch <= 1_000);
    }

    @Test
    void refusesFileWithUnexpectedHeader() {
        ChunkedIngestor ingestor = new ChunkedIngestor(new RecordMapper(), sink, checkpoints, 10);
        assertThrows(IllegalArgumentException.class,
                () -> ingestor.ingest("x.csv", new BufferedReader(new StringReader("id,amount\n1,2\n"))));
    }

    static final class CollectingSink implements RecordSink {
        final List<TransactionRecord> records = new ArrayList<>();
        int maxBatch;

        @Override
        public void writeBatch(String fileKey, List<TransactionRecord> batch) {
            maxBatch = Math.max(maxBatch, batch.size());
            records.addAll(batch);
        }
    }

    static final class FailingSink implements RecordSink {
        private final RecordSink delegate;
        private final int failOnCall;
        private int calls;

        FailingSink(RecordSink delegate, int failOnCall) {
            this.delegate = delegate;
            this.failOnCall = failOnCall;
        }

        @Override
        public void writeBatch(String fileKey, List<TransactionRecord> batch) {
            if (++calls == failOnCall) {
                throw new IllegalStateException("simulated database outage");
            }
            delegate.writeBatch(fileKey, batch);
        }
    }
}
