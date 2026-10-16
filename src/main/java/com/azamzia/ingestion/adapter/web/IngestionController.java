package com.azamzia.ingestion.adapter.web;

import com.azamzia.ingestion.adapter.s3.S3FileSource;
import com.azamzia.ingestion.core.ChunkedIngestor;
import com.azamzia.ingestion.core.IngestionReport;
import com.azamzia.ingestion.core.RejectedRow;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@RestController
@RequestMapping("/api/ingestions")
public class IngestionController {

    private static final int MAX_REJECTS_IN_RESPONSE = 100;

    private final S3FileSource source;
    private final ChunkedIngestor ingestor;

    public IngestionController(S3FileSource source, ChunkedIngestor ingestor) {
        this.source = source;
        this.ingestor = ingestor;
    }

    @PostMapping
    public IngestionResponse ingest(@Valid @RequestBody IngestionRequest request) {
        String fileKey = request.bucket() + "/" + request.key();
        try (BufferedReader reader = source.open(request.bucket(), request.key())) {
            IngestionReport r = ingestor.ingest(fileKey, reader);
            List<RejectedRow> sample = r.rejectedRows().stream().limit(MAX_REJECTS_IN_RESPONSE).toList();
            return new IngestionResponse(fileKey, r.resumedAfterLine(), r.rowsRead(), r.accepted(), r.rejected(),
                    r.batchesCommitted(), r.balanced(), sample);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A malformed file (e.g. wrong header) is a client problem, not a server error. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badFile(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    public record IngestionRequest(@NotBlank String bucket, @NotBlank String key) { }

    public record IngestionResponse(String fileKey, long resumedAfterLine, long rowsRead, long accepted,
                                    long rejected, int batchesCommitted, boolean balanced,
                                    List<RejectedRow> rejectedSample) { }
}
