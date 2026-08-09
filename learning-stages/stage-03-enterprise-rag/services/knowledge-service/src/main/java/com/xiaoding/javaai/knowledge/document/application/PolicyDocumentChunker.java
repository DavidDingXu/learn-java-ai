package com.xiaoding.javaai.knowledge.document.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PolicyDocumentChunker {

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
    private static final Pattern CLAUSE = Pattern.compile("^(第[一二三四五六七八九十百千万0-9]+条)");
    private static final Pattern SENTENCE = Pattern.compile(".*?[。！？!?；;](?:\\s+|$)|.+$");

    private final int maxCharacters;

    public PolicyDocumentChunker(int maxCharacters) {
        if (maxCharacters < 20) throw new IllegalArgumentException("maxCharacters must be at least 20");
        this.maxCharacters = maxCharacters;
    }

    public List<DocumentChunk> chunk(ChunkDocumentCommand command) {
        List<DraftChunk> drafts = parse(command.text());
        List<DocumentChunk> chunks = new ArrayList<>();
        int ordinal = 0;
        for (DraftChunk draft : drafts) {
            for (String part : split(draft.text())) {
                ordinal += 1;
                chunks.add(new DocumentChunk(
                        chunkId(command, ordinal, draft.headingPath(), part),
                        command.tenantId(),
                        command.documentId(),
                        command.documentVersion(),
                        command.chunkPolicyVersion(),
                        ordinal,
                        draft.headingPath(),
                        clause(part),
                        part
                ));
            }
        }
        return List.copyOf(chunks);
    }

    private static List<DraftChunk> parse(String source) {
        String normalized = source.replace("\r\n", "\n").replace('\r', '\n').strip();
        List<String> headings = new ArrayList<>();
        List<DraftChunk> chunks = new ArrayList<>();
        StringBuilder paragraph = new StringBuilder();
        for (String rawLine : normalized.split("\n")) {
            String line = rawLine.stripTrailing();
            Matcher heading = HEADING.matcher(line);
            if (heading.matches()) {
                flush(chunks, headings, paragraph);
                int level = heading.group(1).length();
                while (headings.size() >= level) headings.removeLast();
                while (headings.size() < level - 1) headings.add("");
                headings.add(heading.group(2).strip());
            } else if (line.isBlank()) {
                flush(chunks, headings, paragraph);
            } else {
                if (!paragraph.isEmpty()) paragraph.append('\n');
                paragraph.append(line.strip());
            }
        }
        flush(chunks, headings, paragraph);
        return chunks;
    }

    private static void flush(List<DraftChunk> chunks, List<String> headings, StringBuilder paragraph) {
        if (paragraph.isEmpty()) return;
        chunks.add(new DraftChunk(
                headings.stream().filter(heading -> !heading.isBlank()).toList(),
                paragraph.toString().strip()
        ));
        paragraph.setLength(0);
    }

    private List<String> split(String text) {
        if (text.length() <= maxCharacters) return List.of(text);
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        Matcher matcher = SENTENCE.matcher(text);
        while (matcher.find()) {
            String sentence = matcher.group().strip();
            if (sentence.isEmpty()) continue;
            if (sentence.length() > maxCharacters) {
                flushPart(parts, current);
                hardSplit(parts, sentence);
            } else if (!current.isEmpty() && current.length() + sentence.length() > maxCharacters) {
                flushPart(parts, current);
                current.append(sentence);
            } else {
                current.append(sentence);
            }
        }
        flushPart(parts, current);
        return parts;
    }

    private void hardSplit(List<String> parts, String text) {
        for (int start = 0; start < text.length(); start += maxCharacters) {
            parts.add(text.substring(start, Math.min(text.length(), start + maxCharacters)));
        }
    }

    private static void flushPart(List<String> parts, StringBuilder current) {
        if (current.isEmpty()) return;
        parts.add(current.toString());
        current.setLength(0);
    }

    private static String clause(String text) {
        Matcher matcher = CLAUSE.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String chunkId(
            ChunkDocumentCommand command,
            int ordinal,
            List<String> headingPath,
            String text
    ) {
        StringBuilder material = new StringBuilder();
        append(material, "tenantId");
        append(material, command.tenantId().value());
        append(material, "documentId");
        append(material, command.documentId().value());
        append(material, "documentVersion");
        append(material, Integer.toString(command.documentVersion()));
        append(material, "chunkPolicyVersion");
        append(material, command.chunkPolicyVersion());
        append(material, "ordinal");
        append(material, Integer.toString(ordinal));
        append(material, "headingPath");
        append(material, Integer.toString(headingPath.size()));
        headingPath.forEach(heading -> append(material, heading));
        append(material, "text");
        append(material, text);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(material.toString().getBytes(StandardCharsets.UTF_8));
            return "chunk-" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    private static void append(StringBuilder material, String value) {
        material.append(value.length()).append(':').append(value).append(';');
    }

    private record DraftChunk(List<String> headingPath, String text) {
        private DraftChunk {
            headingPath = List.copyOf(headingPath);
        }
    }
}
