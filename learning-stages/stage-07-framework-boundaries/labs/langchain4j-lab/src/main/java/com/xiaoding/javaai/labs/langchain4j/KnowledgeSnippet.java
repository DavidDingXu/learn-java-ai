package com.xiaoding.javaai.labs.langchain4j;

public record KnowledgeSnippet(String sourceId, String text) {
    public KnowledgeSnippet {
        if (sourceId == null || sourceId.isBlank() || text == null || text.isBlank()) {
            throw new IllegalArgumentException("sourceId and text must not be blank");
        }
    }
}
