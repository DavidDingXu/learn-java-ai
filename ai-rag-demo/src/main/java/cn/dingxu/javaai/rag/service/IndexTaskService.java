package cn.dingxu.javaai.rag.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class IndexTaskService {

    private final DocumentParser parser;
    private final DocumentChunker chunker;
    private final EmbeddingBatchService embeddingBatchService;
    private final VectorIndex vectorIndex;

    public IndexTaskService(DocumentParser parser,
                            DocumentChunker chunker,
                            EmbeddingProvider embeddingProvider,
                            VectorIndex vectorIndex) {
        this(parser, chunker, new EmbeddingBatchService(embeddingProvider), vectorIndex);
    }

    public IndexTaskService(DocumentParser parser,
                            DocumentChunker chunker,
                            EmbeddingBatchService embeddingBatchService,
                            VectorIndex vectorIndex) {
        this.parser = parser;
        this.chunker = chunker;
        this.embeddingBatchService = embeddingBatchService;
        this.vectorIndex = vectorIndex;
    }

    public IndexTaskResult rebuild(String documentId, String markdown) {
        Instant startedAt = Instant.now();
        String taskId = "idx-" + UUID.randomUUID();
        try {
            ParsedDocument document = parser.parseMarkdown(markdown, placeholderMetadata(documentId));
            List<DocumentChunk> chunks = chunker.chunk(document);
            if (chunks.isEmpty()) {
                return result(taskId, document.metadata().documentId(), document.metadata().version(),
                        IndexTaskStatus.FAILED, 0, "document produced no chunks", startedAt);
            }

            List<EmbeddingResult> embeddings = embeddingBatchService.embed(chunks);
            List<VectorIndexEntry> entries = embeddings.stream()
                    .map(embedding -> new VectorIndexEntry(
                            findChunk(chunks, embedding.chunkId()),
                            embedding.vector()))
                    .toList();
            vectorIndex.upsert(entries);

            return result(taskId, document.metadata().documentId(), document.metadata().version(),
                    IndexTaskStatus.COMPLETED, chunks.size(), "", startedAt);
        } catch (RuntimeException e) {
            return result(taskId, documentId, "unknown",
                    IndexTaskStatus.FAILED, 0, e.getMessage(), startedAt);
        }
    }

    private DocumentMetadata placeholderMetadata(String documentId) {
        return new DocumentMetadata(documentId, "unknown-tenant", documentId, "UNKNOWN", null, "unknown");
    }

    private DocumentChunk findChunk(List<DocumentChunk> chunks, String chunkId) {
        return chunks.stream()
                .filter(chunk -> chunk.chunkId().equals(chunkId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("chunk not found: " + chunkId));
    }

    private IndexTaskResult result(String taskId,
                                   String documentId,
                                   String version,
                                   IndexTaskStatus status,
                                   int chunkCount,
                                   String errorMessage,
                                   Instant startedAt) {
        return new IndexTaskResult(
                taskId,
                documentId,
                version,
                status,
                chunkCount,
                errorMessage,
                startedAt,
                Instant.now()
        );
    }
}
