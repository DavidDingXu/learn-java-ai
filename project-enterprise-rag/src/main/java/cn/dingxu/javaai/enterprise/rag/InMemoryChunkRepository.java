package cn.dingxu.javaai.enterprise.rag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class InMemoryChunkRepository {

    private final List<DocumentChunk> chunks = new ArrayList<>();

    public synchronized void replaceDocumentChunks(String documentId, int version, List<DocumentChunk> newChunks) {
        chunks.removeIf(chunk -> chunk.documentId().equals(documentId));
        chunks.addAll(newChunks);
        chunks.sort(Comparator.comparing(DocumentChunk::documentId).thenComparing(DocumentChunk::ordinal));
    }

    public synchronized List<DocumentChunk> findByDocumentId(String documentId) {
        return chunks.stream()
                .filter(chunk -> chunk.documentId().equals(documentId))
                .sorted(Comparator.comparing(DocumentChunk::ordinal))
                .toList();
    }

    public synchronized List<DocumentChunk> findAll() {
        return List.copyOf(chunks);
    }
}
