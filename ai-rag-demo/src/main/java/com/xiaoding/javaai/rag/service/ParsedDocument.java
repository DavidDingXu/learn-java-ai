package com.xiaoding.javaai.rag.service;

public record ParsedDocument(
        DocumentMetadata metadata,
        String markdown,
        String plainText
) {
    public ParsedDocument {
        if (metadata == null) {
            throw new IllegalArgumentException("metadata must not be null");
        }
        markdown = markdown == null ? "" : markdown;
        plainText = plainText == null ? "" : plainText;
    }
}
