package com.xiaoding.javaai.customer.consultation.domain;

public record CitationView(
        String documentId,
        String version,
        String sectionId,
        String title
) {
    public CitationView {
        documentId = requireText(documentId, "documentId");
        version = requireText(version, "version");
        sectionId = requireText(sectionId, "sectionId");
        title = requireText(title, "title");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
