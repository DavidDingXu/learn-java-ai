package com.xiaoding.javaai.rag.service;

public interface DocumentParser {

    ParsedDocument parseMarkdown(String markdown, DocumentMetadata metadata);

    ParsedDocument parsePlainText(String plainText, DocumentMetadata metadata);
}
