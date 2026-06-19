package com.xiaoding.javaai.rag.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ContextCompressor {

    public ContextCompressionResult compress(List<MultiQueryRetrievalItem> items, int maxCharsPerChunk) {
        if (items == null || items.isEmpty()) {
            return new ContextCompressionResult(List.of(), List.of());
        }
        if (maxCharsPerChunk <= 0) {
            return new ContextCompressionResult(
                    List.of(),
                    items.stream().map(item -> item.chunk().chunkId()).toList()
            );
        }

        List<CompressedContextItem> compressed = new ArrayList<>();
        List<String> dropped = new ArrayList<>();
        for (MultiQueryRetrievalItem item : items) {
            DocumentChunk chunk = item.chunk();
            String content = chunk.content() == null ? "" : chunk.content();
            if (content.isBlank()) {
                dropped.add(chunk.chunkId());
                continue;
            }
            compressed.add(new CompressedContextItem(
                    chunk.documentId(),
                    chunk.chunkId(),
                    truncate(content, maxCharsPerChunk)
            ));
        }
        return new ContextCompressionResult(compressed, dropped);
    }

    private String truncate(String content, int maxChars) {
        if (content.length() <= maxChars) {
            return content;
        }
        if (maxChars <= 1) {
            return content.substring(0, maxChars);
        }
        return content.substring(0, maxChars - 1) + "…";
    }
}
