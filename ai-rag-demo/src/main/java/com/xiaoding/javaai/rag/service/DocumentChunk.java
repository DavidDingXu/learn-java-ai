package com.xiaoding.javaai.rag.service;

import java.util.Set;
import java.util.List;

public record DocumentChunk(
        String documentId,
        String chunkId,
        String tenantId,
        Set<String> departments,
        List<String> headingPath,
        String docType,
        String version,
        String content
) {
    public DocumentChunk(String documentId, String chunkId, String tenantId, Set<String> departments, String content) {
        this(documentId, chunkId, tenantId, departments, List.of(), "UNKNOWN", "v1", content);
    }

    public DocumentChunk {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId must not be blank");
        }
        if (chunkId == null || chunkId.isBlank()) {
            throw new IllegalArgumentException("chunkId must not be blank");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        departments = departments == null ? Set.of() : Set.copyOf(departments);
        headingPath = headingPath == null ? List.of() : List.copyOf(headingPath);
        docType = docType == null || docType.isBlank() ? "UNKNOWN" : docType;
        version = version == null || version.isBlank() ? "v1" : version;
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
    }

    public boolean accessibleBy(OperatorScope scope) {
        return tenantId.equals(scope.tenantId()) && departments.contains(scope.department());
    }
}
