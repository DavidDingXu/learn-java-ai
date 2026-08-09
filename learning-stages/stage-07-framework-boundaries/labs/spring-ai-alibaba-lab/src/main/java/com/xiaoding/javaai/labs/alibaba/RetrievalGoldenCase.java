package com.xiaoding.javaai.labs.alibaba;

import java.time.Duration;
import java.util.List;

public record RetrievalGoldenCase(
        String caseId,
        List<String> expectedDocumentIds,
        List<String> retrievedDocumentIds,
        Duration latency) {

    public RetrievalGoldenCase {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("caseId must not be blank");
        }
        expectedDocumentIds = List.copyOf(expectedDocumentIds);
        retrievedDocumentIds = List.copyOf(retrievedDocumentIds);
        if (expectedDocumentIds.isEmpty()) {
            throw new IllegalArgumentException("expectedDocumentIds must not be empty");
        }
        if (latency == null || latency.isNegative()) {
            throw new IllegalArgumentException("latency must not be negative");
        }
    }
}
