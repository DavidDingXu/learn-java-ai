package com.xiaoding.javaai.knowledge.document.web;

import com.xiaoding.javaai.knowledge.document.application.PublishedDocument;

import java.util.UUID;

public record PublishedDocumentResponse(
        String documentId,
        int versionNumber,
        long revision,
        UUID indexTaskId,
        String indexStatus
) {
    static PublishedDocumentResponse from(PublishedDocument document) {
        return new PublishedDocumentResponse(
                document.documentId().value(), document.versionNumber(), document.revision(),
                document.indexTaskId(), "PENDING"
        );
    }
}
