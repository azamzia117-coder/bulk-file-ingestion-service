package com.azamzia.ingestion.core;

import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

/** Remembers the last fully committed line per file so an interrupted ingestion can resume. */
public interface CheckpointStore {

    OptionalLong lastCommittedLine(String fileKey);

    void commit(String fileKey, long lineNumber);

    final class InMemory implements CheckpointStore {
        private final Map<String, Long> lines = new ConcurrentHashMap<>();

        @Override
        public OptionalLong lastCommittedLine(String fileKey) {
            Long v = lines.get(fileKey);
            return v == null ? OptionalLong.empty() : OptionalLong.of(v);
        }

        @Override
        public void commit(String fileKey, long lineNumber) {
            lines.put(fileKey, lineNumber);
        }
    }
}
