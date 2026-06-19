package cn.dingxu.javaai.enterprise.rag;

import java.time.Instant;
import java.util.Set;

public record DocumentChunk(
        String documentId,
        String chunkId,
        String tenantId,
        Set<String> departments,
        DocumentType type,
        int version,
        int ordinal,
        String topic,
        int priority,
        Instant effectiveFrom,
        Instant effectiveTo,
        String content
) {
    public DocumentChunk(String documentId,
                         String chunkId,
                         String tenantId,
                         Set<String> departments,
                         DocumentType type,
                         int version,
                         int ordinal,
                         String content) {
        this(documentId, chunkId, tenantId, departments, type, version, ordinal, "general", 0, null, null, content);
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
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
        if (ordinal < 1) {
            throw new IllegalArgumentException("ordinal must be positive");
        }
        topic = topic == null || topic.isBlank() ? "general" : topic;
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
    }

    boolean accessibleBy(OperatorScope scope) {
        return tenantId.equals(scope.tenantId()) && departments.contains(scope.department());
    }
}
