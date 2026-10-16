package com.azamzia.ingestion.core;

import java.util.ArrayList;
import java.util.List;

/** Minimal RFC 4180 parser for a single line: supports quoted fields and escaped quotes. */
public final class CsvLineParser {

    private CsvLineParser() {
    }

    public static List<String> parse(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    inQuotes = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (inQuotes) {
            throw new IllegalArgumentException("unterminated quoted field");
        }
        fields.add(current.toString());
        return fields;
    }
}
