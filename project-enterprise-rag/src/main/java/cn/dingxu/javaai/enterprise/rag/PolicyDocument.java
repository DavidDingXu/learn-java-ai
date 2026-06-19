package cn.dingxu.javaai.enterprise.rag;

import java.time.Instant;
import java.util.Set;

public record PolicyDocument(
        String documentId,
        String tenantId,
        Set<String> departments,
        DocumentType type,
        int version,
        String content,
        Instant updatedAt
) {
    public PolicyDocument {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId must not be blank");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        departments = departments == null ? Set.of() : Set.copyOf(departments);
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    boolean accessibleBy(OperatorScope scope) {
        return tenantId.equals(scope.tenantId()) && departments.contains(scope.department());
    }
}
