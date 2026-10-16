package com.azamzia.ingestion.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Streams a CSV file line by line (constant memory regardless of file size), validates each row, writes
 * accepted rows in batches and commits a checkpoint only after a batch is durably written.
 *
 * <p>If the process dies mid-file, the next run skips to the last checkpoint, so rows are neither lost
 * nor written twice.
 */
public class ChunkedIngestor {

    private static final System.Logger LOG = System.getLogger(ChunkedIngestor.class.getName());

    private final RecordMapper mapper;
    private final RecordSink sink;
    private final CheckpointStore checkpoints;
    private final int batchSize;

    public ChunkedIngestor(RecordMapper mapper, RecordSink sink, CheckpointStore checkpoints, int batchSize) {
        if (batchSize < 1) throw new IllegalArgumentException("batchSize must be >= 1");
        this.mapper = mapper;
        this.sink = sink;
        this.checkpoints = checkpoints;
        this.batchSize = batchSize;
    }

    public IngestionReport ingest(String fileKey, BufferedReader reader) {
        long resumeAfter = checkpoints.lastCommittedLine(fileKey).orElse(1L); // line 1 is the header
        long lineNo = 0;
        long read = 0;
        long accepted = 0;
        int batches = 0;
        List<TransactionRecord> batch = new ArrayList<>(batchSize);
        List<RejectedRow> rejected = new ArrayList<>();
        List<RejectedRow> pendingRejects = new ArrayList<>();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (lineNo == 1) {
                    verifyHeader(line);
                    continue;
                }
                if (lineNo <= resumeAfter || line.isBlank()) {
                    continue;
                }
                read++;
                RecordMapper.Result result;
                try {
                    result = mapper.map(CsvLineParser.parse(line));
                } catch (IllegalArgumentException e) {
                    result = new RecordMapper.Result(null, List.of(e.getMessage()));
                }
                if (result.ok()) {
                    batch.add(result.record());
                } else {
                    pendingRejects.add(new RejectedRow(lineNo, line, result.errors()));
                }
                if (batch.size() + pendingRejects.size() >= batchSize) {
                    accepted += flush(fileKey, batch, lineNo);
                    rejected.addAll(pendingRejects);
                    pendingRejects.clear();
                    batches++;
                }
            }
            if (!batch.isEmpty() || !pendingRejects.isEmpty()) {
                accepted += flush(fileKey, batch, lineNo);
                rejected.addAll(pendingRejects);
                batches++;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("failed reading " + fileKey + " at line " + lineNo, e);
        }
        IngestionReport report = new IngestionReport(fileKey, resumeAfter, read, accepted, rejected.size(), batches, rejected);
        LOG.log(System.Logger.Level.INFO, "Ingested {0}: read={1} accepted={2} rejected={3}",
                fileKey, read, accepted, rejected.size());
        return report;
    }

    private long flush(String fileKey, List<TransactionRecord> batch, long lastLine) {
        int n = batch.size();
        if (n > 0) {
            sink.writeBatch(fileKey, List.copyOf(batch)); // throws => checkpoint not advanced
        }
        checkpoints.commit(fileKey, lastLine);
        batch.clear();
        return n;
    }

    private static void verifyHeader(String line) {
        List<String> header = CsvLineParser.parse(line).stream().map(String::trim).toList();
        if (!header.equals(RecordMapper.HEADER)) {
            throw new IllegalArgumentException("unexpected header " + header + ", expected " + RecordMapper.HEADER);
        }
    }
}
