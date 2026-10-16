package com.azamzia.ingestion.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvLineParserTest {

    @Test
    void splitsSimpleLine() {
        assertEquals(List.of("a", "b", "", "d"), CsvLineParser.parse("a,b,,d"));
    }

    @Test
    void handlesQuotedCommasAndEscapedQuotes() {
        assertEquals(List.of("x", "hello, \"world\"", "z"), CsvLineParser.parse("x,\"hello, \"\"world\"\"\",z"));
    }

    @Test
    void rejectsUnterminatedQuote() {
        assertThrows(IllegalArgumentException.class, () -> CsvLineParser.parse("a,\"oops"));
    }
}
