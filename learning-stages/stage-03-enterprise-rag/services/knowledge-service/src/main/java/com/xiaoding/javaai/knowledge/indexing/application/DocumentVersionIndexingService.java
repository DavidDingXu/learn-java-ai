package com.xiaoding.javaai.knowledge.indexing.application;

import com.xiaoding.javaai.knowledge.document.application.ChunkDocumentCommand;
import com.xiaoding.javaai.knowledge.document.application.PolicyDocumentChunker;
import com.xiaoding.javaai.knowledge.indexing.application.port.DocumentVersionIndexer;
import com.xiaoding.javaai.knowledge.indexing.application.port.DocumentVersionTextSource;
import com.xiaoding.javaai.knowledge.indexing.application.port.KnowledgeChunkIndexSink;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeEmbedding;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeEmbeddingModel;

import java.util.ArrayList;
import java.util.List;

public final class DocumentVersionIndexingService implements DocumentVersionIndexer {

    private final DocumentVersionTextSource source;
    private final PolicyDocumentChunker chunker;
    private final KnowledgeEmbeddingModel embeddingModel;
    private final KnowledgeChunkIndexSink sink;
    private final String chunkPolicyVersion;
    private final int embeddingBatchSize;

    public DocumentVersionIndexingService(
            DocumentVersionTextSource source,
            PolicyDocumentChunker chunker,
            KnowledgeEmbeddingModel embeddingModel,
            KnowledgeChunkIndexSink sink,
            String chunkPolicyVersion,
            int embeddingBatchSize
    ) {
        if (chunkPolicyVersion == null || chunkPolicyVersion.isBlank()) {
            throw new IllegalArgumentException("chunkPolicyVersion must not be blank");
        }
        if (embeddingBatchSize < 1) {
            throw new IllegalArgumentException("embeddingBatchSize must be positive");
        }
        this.source = source;
        this.chunker = chunker;
        this.embeddingModel = embeddingModel;
        this.sink = sink;
        this.chunkPolicyVersion = chunkPolicyVersion.strip();
        this.embeddingBatchSize = embeddingBatchSize;
    }

    @Override
    public void index(ClaimedIndexTask task) {
        if (task.taskType() != IndexTaskType.REINDEX_DOCUMENT_VERSION) {
            throw new IndexingException("UNSUPPORTED_INDEX_TASK", "unsupported task type " + task.taskType());
        }
        try {
            String text = source.loadText(task);
            var chunks = chunker.chunk(new ChunkDocumentCommand(
                    task.tenantId(),
                    task.documentId(),
                    task.documentVersion(),
                    chunkPolicyVersion,
                    text
            ));
            if (chunks.isEmpty()) {
                throw new IndexingException("DOCUMENT_PRODUCED_NO_CHUNKS", "document produced no chunks");
            }
            List<KnowledgeEmbedding> embeddings = new ArrayList<>(chunks.size());
            String embeddingModelName = null;
            for (int start = 0; start < chunks.size(); start += embeddingBatchSize) {
                int end = Math.min(chunks.size(), start + embeddingBatchSize);
                List<KnowledgeEmbedding> batch = embeddingModel.embedAll(
                        chunks.subList(start, end).stream().map(chunk -> chunk.text()).toList()
                );
                if (batch.size() != end - start) {
                    throw new IndexingException(
                            "EMBEDDING_COUNT_MISMATCH",
                            "embedding count does not match chunk count"
                    );
                }
                for (KnowledgeEmbedding embedding : batch) {
                    if (embeddingModelName == null) embeddingModelName = embedding.model();
                    if (!embeddingModelName.equals(embedding.model())) {
                        throw new IndexingException(
                                "EMBEDDING_MODEL_MISMATCH",
                                "one document version must use one embedding model"
                        );
                    }
                }
                embeddings.addAll(batch);
            }
            sink.replaceVersion(task, chunks, List.copyOf(embeddings));
        } catch (IndexingException error) {
            throw error;
        } catch (RuntimeException error) {
            throw new IndexingException("INDEX_DOCUMENT_FAILED", error.getMessage());
        }
    }
}
