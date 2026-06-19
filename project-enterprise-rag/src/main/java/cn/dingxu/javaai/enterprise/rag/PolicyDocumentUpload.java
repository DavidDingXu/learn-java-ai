package cn.dingxu.javaai.enterprise.rag;

import java.util.Set;

public record PolicyDocumentUpload(
        String documentId,
        String tenantId,
        Set<String> departments,
        DocumentType type,
        String content
) {
    public PolicyDocumentUpload {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId must not be blank");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        departments = departments == null ? Set.of() : Set.copyOf(departments);
        if (departments.isEmpty()) {
            throw new IllegalArgumentException("departments must not be empty");
        }
        type = type == null ? DocumentType.POLICY : type;
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
    }
}
