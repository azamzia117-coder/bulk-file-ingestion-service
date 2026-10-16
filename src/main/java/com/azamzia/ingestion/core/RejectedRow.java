package com.azamzia.ingestion.core;

import java.util.List;

/** A row that failed validation, kept with its line number so it can be corrected and replayed. */
public record RejectedRow(long lineNumber, String rawLine, List<String> errors) {
}
