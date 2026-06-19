package com.xiaoding.javaai.rag.service;

import java.util.Set;

public record DocumentMetadata(
        String documentId,
        String tenantId,
        String title,
        String docType,
        Set<String> departments,
        String version
) {
    public DocumentMetadata {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId must not be blank");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        title = title == null || title.isBlank() ? documentId : title;
        docType = docType == null || docType.isBlank() ? "UNKNOWN" : docType;
        departments = departments == null ? Set.of() : Set.copyOf(departments);
        version = version == null || version.isBlank() ? "v1" : version;
    }
}
