package com.xiaoding.javaai.enterprise.rag;

public record Citation(String documentId, String chunkId, String quote) {
    public Citation {
        quote = quote == null ? "" : quote;
    }
}
