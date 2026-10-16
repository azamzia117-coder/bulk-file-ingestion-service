package com.azamzia.ingestion.core;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

/**
 * Converts parsed CSV fields into a {@link TransactionRecord}, collecting every problem with the row
 * instead of stopping at the first one so rejected-row reports are actionable.
 */
public class RecordMapper {

    public static final List<String> HEADER = List.of("record_id", "account_id", "amount_minor", "currency", "posted_date");

    public Result map(List<String> f) {
        List<String> errors = new ArrayList<>();
        if (f.size() != HEADER.size()) {
            errors.add("expected " + HEADER.size() + " columns but found " + f.size());
            return new Result(null, errors);
        }
        String recordId = f.get(0).trim();
        String accountId = f.get(1).trim();
        if (recordId.isEmpty()) errors.add("record_id is required");
        if (!accountId.matches("ACC-\\d{6}")) errors.add("account_id must match ACC-######");
        long amount = 0;
        try {
            amount = Long.parseLong(f.get(2).trim());
            if (amount == 0) errors.add("amount_minor must be non-zero");
        } catch (NumberFormatException e) {
            errors.add("amount_minor is not an integer");
        }
        String currency = f.get(3).trim();
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException e) {
            errors.add("currency is not ISO-4217");
        }
        LocalDate posted = null;
        try {
            posted = LocalDate.parse(f.get(4).trim());
        } catch (DateTimeParseException e) {
            errors.add("posted_date must be yyyy-MM-dd");
        }
        return errors.isEmpty()
                ? new Result(new TransactionRecord(recordId, accountId, amount, currency, posted), errors)
                : new Result(null, errors);
    }

    public record Result(TransactionRecord record, List<String> errors) {
        public boolean ok() {
            return errors.isEmpty();
        }
    }
}
