package com.azamzia.ingestion.core;

import java.time.LocalDate;

/** One validated row from a synthetic transaction file. Amount is in minor units. */
public record TransactionRecord(String recordId, String accountId, long amountMinor, String currency, LocalDate postedDate) {
}
