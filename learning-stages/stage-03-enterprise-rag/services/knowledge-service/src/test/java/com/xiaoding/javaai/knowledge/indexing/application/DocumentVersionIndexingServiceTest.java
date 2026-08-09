package com.xiaoding.javaai.knowledge.indexing.application;

import com.xiaoding.javaai.knowledge.document.application.DocumentChunk;
import com.xiaoding.javaai.knowledge.document.application.PolicyDocumentChunker;
import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.indexing.application.port.DocumentVersionTextSource;
import com.xiaoding.javaai.knowledge.indexing.application.port.KnowledgeChunkIndexSink;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeEmbedding;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeEmbeddingModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentVersionIndexingServiceTest {

    @Test
    void deterministically_chunks_embeds_and_replaces_one_document_version() {
        RecordingSink sink = new RecordingSink();
        BatchRecordingEmbeddingModel embeddingModel = new BatchRecordingEmbeddingModel();
        DocumentVersionIndexingService service = new DocumentVersionIndexingService(
                task -> "# 退款政策\n\n第十条 退款审核通过后，一到五个工作日原路到账。",
                new PolicyDocumentChunker(200),
                embeddingModel,
                sink,
                "policy-chunk-v1",
                16
        );
        ClaimedIndexTask task = task();

        service.index(task);
        List<String> firstChunkIds = sink.chunks.stream().map(DocumentChunk::chunkId).toList();
        service.index(task);

        assertThat(sink.task).isEqualTo(task);
        assertThat(sink.chunks).singleElement().satisfies(chunk -> {
            assertThat(chunk.headingPath()).containsExactly("退款政策");
            assertThat(chunk.clause()).isEqualTo("第十条");
        });
        assertThat(sink.embeddings).singleElement().satisfies(embedding ->
                assertThat(embedding.model()).isEqualTo("embedding-v1"));
        assertThat(sink.chunks).extracting(DocumentChunk::chunkId)
                .containsExactlyElementsOf(firstChunkIds);
        assertThat(sink.replaceCalls).isEqualTo(2);
        assertThat(embeddingModel.batchCalls).isEqualTo(2);
        assertThat(embeddingModel.singleCalls).isZero();
    }

    @Test
    void limitsEachProviderRequestToTheConfiguredBatchSize() {
        BatchRecordingEmbeddingModel embeddingModel = new BatchRecordingEmbeddingModel();
        DocumentVersionIndexingService service = new DocumentVersionIndexingService(
                task -> """
                        # 退款政策
                        第一段说明。

                        第二段说明。

                        第三段说明。
                        """,
                new PolicyDocumentChunker(200),
                embeddingModel,
                new RecordingSink(),
                "policy-chunk-v1",
                2
        );

        service.index(task());

        assertThat(embeddingModel.batchSizes).containsExactly(2, 1);
        assertThat(embeddingModel.singleCalls).isZero();
    }

    private static ClaimedIndexTask task() {
        return new ClaimedIndexTask(
                UUID.fromString("00000000-0000-0000-0000-000000000020"),
                new TenantId("tenant-a"),
                new DocumentId("refund-policy"),
                2,
                IndexTaskType.REINDEX_DOCUMENT_VERSION,
                1
        );
    }

    private static final class RecordingSink implements KnowledgeChunkIndexSink {
        private ClaimedIndexTask task;
        private List<DocumentChunk> chunks;
        private List<KnowledgeEmbedding> embeddings;
        private int replaceCalls;

        @Override
        public void replaceVersion(
                ClaimedIndexTask task,
                List<DocumentChunk> chunks,
                List<KnowledgeEmbedding> embeddings
        ) {
            this.task = task;
            this.chunks = List.copyOf(chunks);
            this.embeddings = List.copyOf(embeddings);
            replaceCalls += 1;
        }
    }

    private static final class BatchRecordingEmbeddingModel implements KnowledgeEmbeddingModel {
        private int batchCalls;
        private int singleCalls;
        private final java.util.ArrayList<Integer> batchSizes = new java.util.ArrayList<>();

        @Override
        public KnowledgeEmbedding embed(String text) {
            singleCalls += 1;
            return new KnowledgeEmbedding(new float[]{text.length(), 1.0f}, "embedding-v1");
        }

        @Override
        public List<KnowledgeEmbedding> embedAll(List<String> texts) {
            batchCalls += 1;
            batchSizes.add(texts.size());
            return texts.stream()
                    .map(text -> new KnowledgeEmbedding(
                            new float[]{text.length(), 1.0f}, "embedding-v1"))
                    .toList();
        }
    }
}
