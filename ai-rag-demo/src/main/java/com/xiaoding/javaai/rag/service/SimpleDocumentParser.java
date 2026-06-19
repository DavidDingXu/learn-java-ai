package com.xiaoding.javaai.rag.service;

import org.springframework.stereotype.Service;

@Service
public class SimpleDocumentParser implements DocumentParser {

    @Override
    public ParsedDocument parseMarkdown(String markdown, DocumentMetadata metadata) {
        String normalizedMarkdown = normalize(markdown);
        return new ParsedDocument(metadata, normalizedMarkdown, toPlainText(normalizedMarkdown));
    }

    @Override
    public ParsedDocument parsePlainText(String plainText, DocumentMetadata metadata) {
        String normalizedText = normalize(plainText);
        return new ParsedDocument(metadata, normalizedText, normalizedText);
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").replace("\r", "\n").trim();
    }

    private String toPlainText(String markdown) {
        return markdown.lines()
                .map(line -> line.replaceFirst("^#{1,6}\\s+", ""))
                .map(line -> line.replace("**", "").replace("__", ""))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }
}
