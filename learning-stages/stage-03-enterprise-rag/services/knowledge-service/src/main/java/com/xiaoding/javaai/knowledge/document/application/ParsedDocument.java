package com.xiaoding.javaai.knowledge.document.application;

public record ParsedDocument(String text) {
    public ParsedDocument {
        if (text == null || text.isBlank()) throw new DocumentParsingException("parsed document is blank");
    }
}
