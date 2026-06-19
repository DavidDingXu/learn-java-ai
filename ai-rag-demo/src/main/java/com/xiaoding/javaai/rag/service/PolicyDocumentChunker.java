package com.xiaoding.javaai.rag.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PolicyDocumentChunker implements DocumentChunker {

    private final int maxChunkChars;

    public PolicyDocumentChunker() {
        this(200);
    }

    public PolicyDocumentChunker(int maxChunkChars) {
        this.maxChunkChars = Math.max(20, maxChunkChars);
    }

    @Override
    public List<DocumentChunk> chunk(ParsedDocument document) {
        List<Section> sections = sections(document.markdown());
        List<DocumentChunk> chunks = new ArrayList<>();
        int sequence = 1;
        for (Section section : sections) {
            for (String part : split(section.content())) {
                chunks.add(toChunk(document.metadata(), section.headingPath(), part, sequence));
                sequence++;
            }
        }
        return chunks;
    }

    private DocumentChunk toChunk(DocumentMetadata metadata, List<String> headingPath, String content, int sequence) {
        return new DocumentChunk(
                metadata.documentId(),
                metadata.documentId() + "-c" + sequence,
                metadata.tenantId(),
                metadata.departments(),
                headingPath,
                metadata.docType(),
                metadata.version(),
                content
        );
    }

    private List<Section> sections(String markdown) {
        List<Section> sections = new ArrayList<>();
        List<String> headingPath = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : markdown.lines().toList()) {
            Heading heading = parseHeading(line);
            if (heading != null) {
                flushSection(sections, headingPath, current);
                applyHeading(headingPath, heading);
            } else if (!line.isBlank()) {
                current.append(line.trim()).append('\n');
            }
        }
        flushSection(sections, headingPath, current);
        return sections;
    }

    private void applyHeading(List<String> headingPath, Heading heading) {
        while (headingPath.size() >= heading.level()) {
            headingPath.remove(headingPath.size() - 1);
        }
        headingPath.add(heading.title());
    }

    private Heading parseHeading(String line) {
        String trimmed = line.trim();
        if (!trimmed.startsWith("#")) {
            return null;
        }
        int level = 0;
        while (level < trimmed.length() && trimmed.charAt(level) == '#') {
            level++;
        }
        if (level == 0 || level > 6 || level >= trimmed.length() || trimmed.charAt(level) != ' ') {
            return null;
        }
        String title = trimmed.substring(level + 1).trim();
        if (title.isBlank()) {
            return null;
        }
        return new Heading(level, title);
    }

    private void flushSection(List<Section> sections, List<String> headingPath, StringBuilder current) {
        String content = current.toString().trim();
        if (!content.isBlank()) {
            sections.add(new Section(List.copyOf(headingPath), content));
            current.setLength(0);
        }
    }

    private List<String> split(String content) {
        if (content.length() <= maxChunkChars) {
            return List.of(content);
        }
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + maxChunkChars, content.length());
            if (end < content.length()) {
                end = sentenceBoundary(content, start, end);
            }
            parts.add(content.substring(start, end).trim());
            start = end;
        }
        return parts.stream().filter(part -> !part.isBlank()).toList();
    }

    private int sentenceBoundary(String content, int start, int defaultEnd) {
        int forwardLimit = Math.min(content.length(), defaultEnd + 4);
        for (int i = defaultEnd; i < forwardLimit; i++) {
            char c = content.charAt(i);
            if (c == '。' || c == '；' || c == ';' || c == '\n') {
                return i + 1;
            }
        }
        for (int i = defaultEnd; i > start; i--) {
            char c = content.charAt(i - 1);
            if (c == '。' || c == '；' || c == ';' || c == '\n') {
                return i;
            }
        }
        return defaultEnd;
    }

    private record Heading(int level, String title) {
    }

    private record Section(List<String> headingPath, String content) {
    }
}
