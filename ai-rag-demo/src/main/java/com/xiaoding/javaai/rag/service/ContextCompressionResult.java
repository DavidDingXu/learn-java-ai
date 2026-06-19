package com.xiaoding.javaai.rag.service;

import java.util.List;

public record ContextCompressionResult(
        List<CompressedContextItem> items,
        List<String> droppedChunkIds
) {
    public ContextCompressionResult {
        items = items == null ? List.of() : List.copyOf(items);
        droppedChunkIds = droppedChunkIds == null ? List.of() : List.copyOf(droppedChunkIds);
    }
}
