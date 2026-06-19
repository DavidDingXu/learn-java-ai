package cn.dingxu.javaai.enterprise.rag;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PolicyDocumentChunker {

    private static final Pattern META_PREFIX = Pattern.compile("^(#\\S+(?:\\s+\\S+=\\S+)*)\\s+(.+)$");
    private static final Pattern META_PAIR = Pattern.compile("(topic|priority|effectiveFrom|effectiveTo)=([^\\s]+)");

    public List<DocumentChunk> chunk(PolicyDocument document) {
        String[] lines = document.content().split("\\R+");
        List<DocumentChunk> chunks = new ArrayList<>();
        int ordinal = 1;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isBlank()) {
                continue;
            }
            ChunkLine chunkLine = parseLine(line);
            chunks.add(new DocumentChunk(
                    document.documentId(),
                    document.documentId() + "-v" + document.version() + "-c" + ordinal,
                    document.tenantId(),
                    document.departments(),
                    document.type(),
                    document.version(),
                    ordinal,
                    chunkLine.topic(),
                    chunkLine.priority(),
                    chunkLine.effectiveFrom(),
                    chunkLine.effectiveTo(),
                    chunkLine.content()
            ));
            ordinal++;
        }
        if (chunks.isEmpty()) {
            chunks.add(new DocumentChunk(
                    document.documentId(),
                    document.documentId() + "-v" + document.version() + "-c1",
                    document.tenantId(),
                    document.departments(),
                    document.type(),
                    document.version(),
                    1,
                    "general",
                    0,
                    null,
                    null,
                    document.content().trim()
            ));
        }
        return chunks;
    }

    private ChunkLine parseLine(String line) {
        Matcher matcher = META_PREFIX.matcher(line);
        if (!matcher.matches()) {
            return new ChunkLine("general", 0, null, null, line);
        }
        String meta = matcher.group(1);
        String content = matcher.group(2);
        String topic = "general";
        int priority = 0;
        Instant effectiveFrom = null;
        Instant effectiveTo = null;

        Matcher pairMatcher = META_PAIR.matcher(meta);
        while (pairMatcher.find()) {
            String key = pairMatcher.group(1);
            String value = pairMatcher.group(2);
            switch (key) {
                case "topic" -> topic = value;
                case "priority" -> priority = parsePriority(value);
                case "effectiveFrom" -> effectiveFrom = parseInstant(value);
                case "effectiveTo" -> effectiveTo = parseInstant(value);
                default -> {
                }
            }
        }
        return new ChunkLine(topic, priority, effectiveFrom, effectiveTo, content);
    }

    private int parsePriority(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private record ChunkLine(
            String topic,
            int priority,
            Instant effectiveFrom,
            Instant effectiveTo,
            String content
    ) {
    }
}
