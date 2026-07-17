package com.xiaoding.javaai.knowledge.document.web;

import com.xiaoding.javaai.knowledge.document.application.UploadedDocument;

public record UploadedDocumentResponse(
        String documentId,
        int versionNumber,
        String contentHash,
        long revision
) {
    static UploadedDocumentResponse from(UploadedDocument document) {
        return new UploadedDocumentResponse(
                document.documentId().value(), document.versionNumber(), document.contentHash().value(),
                document.revision()
        );
    }
}
