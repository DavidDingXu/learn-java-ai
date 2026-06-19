package com.xiaoding.javaai.rag.service;

public record CompressedContextItem(
        String documentId,
        String chunkId,
        String compressedContent
) {
    public CompressedContextItem {
        documentId = documentId == null ? "" : documentId;
        chunkId = chunkId == null ? "" : chunkId;
        compressedContent = compressedContent == null ? "" : compressedContent;
    }
}
