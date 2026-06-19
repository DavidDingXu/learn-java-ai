package cn.dingxu.javaai.enterprise.rag;

public record Citation(String documentId, String chunkId, String quote) {
    public Citation {
        quote = quote == null ? "" : quote;
    }
}
