package com.xiaoding.javaai.rag;

import com.xiaoding.javaai.rag.service.ContextCompressor;
import com.xiaoding.javaai.rag.service.ContextCompressionResult;
import com.xiaoding.javaai.rag.service.DocumentChunk;
import com.xiaoding.javaai.rag.service.HybridRetriever;
import com.xiaoding.javaai.rag.service.InMemoryVectorIndex;
import com.xiaoding.javaai.rag.service.MultiQueryRagRetrievalResult;
import com.xiaoding.javaai.rag.service.MultiQueryRagRetriever;
import com.xiaoding.javaai.rag.service.OperatorScope;
import com.xiaoding.javaai.rag.service.QueryRewriteService;
import com.xiaoding.javaai.rag.service.VectorIndexEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MultiQueryRagRetrieverTest {

    @Test
    void retrievesAcrossRewrittenQueriesAndKeepsQuerySources() {
        DocumentChunk refund = chunk("refund-policy", "c1", "发货后退款需先核对物流状态。");
        DocumentChunk manualReview = chunk("refund-policy", "c2", "高金额退款必须转人工复核。");
        InMemoryVectorIndex vectorIndex = new InMemoryVectorIndex();
        vectorIndex.upsert(List.of(
                new VectorIndexEntry(refund, List.of(1.0, 0.0)),
                new VectorIndexEntry(manualReview, List.of(0.9, 0.1))
        ));
        HybridRetriever hybridRetriever = new HybridRetriever(List.of(refund, manualReview), vectorIndex);
        MultiQueryRagRetriever retriever = new MultiQueryRagRetriever(new QueryRewriteService(), hybridRetriever);

        MultiQueryRagRetrievalResult result = retriever.retrieve(
                "客户申请退款，但订单已经发货，应该怎么处理？",
                List.of(1.0, 0.0),
                new OperatorScope("tenant-a", "support"),
                3
        );

        assertThat(result.rewrite().queries()).contains("发货后退款处理规则");
        assertThat(result.results()).extracting(item -> item.chunk().chunkId())
                .contains("c1", "c2");
        assertThat(result.results().stream()
                .filter(item -> item.chunk().chunkId().equals("c1"))
                .findFirst()
                .orElseThrow()
                .matchedQueries()).isNotEmpty();
    }

    @Test
    void compressesContextWithoutDroppingCitationIdentity() {
        DocumentChunk refund = chunk("refund-policy", "c1", "发货后退款需先核对物流状态，确认客户是否拒收、退回或已签收。");
        DocumentChunk manualReview = chunk("refund-policy", "c2", "高金额退款必须转人工复核，系统只能生成建议，不能直接执行退款。");
        InMemoryVectorIndex vectorIndex = new InMemoryVectorIndex();
        vectorIndex.upsert(List.of(
                new VectorIndexEntry(refund, List.of(1.0, 0.0)),
                new VectorIndexEntry(manualReview, List.of(0.9, 0.1))
        ));
        HybridRetriever hybridRetriever = new HybridRetriever(List.of(refund, manualReview), vectorIndex);
        MultiQueryRagRetriever retriever = new MultiQueryRagRetriever(new QueryRewriteService(), hybridRetriever);
        MultiQueryRagRetrievalResult retrieved = retriever.retrieve(
                "客户申请退款，但订单已经发货，应该怎么处理？",
                List.of(1.0, 0.0),
                new OperatorScope("tenant-a", "support"),
                5
        );

        ContextCompressionResult compressed = new ContextCompressor().compress(retrieved.results(), 42);

        assertThat(compressed.items()).hasSize(2);
        assertThat(compressed.items()).allSatisfy(item -> {
            assertThat(item.documentId()).isEqualTo("refund-policy");
            assertThat(item.chunkId()).isIn("c1", "c2");
            assertThat(item.compressedContent().length()).isLessThanOrEqualTo(42);
        });
        assertThat(compressed.droppedChunkIds()).isEmpty();
    }

    private DocumentChunk chunk(String documentId, String chunkId, String content) {
        return new DocumentChunk(
                documentId,
                chunkId,
                "tenant-a",
                Set.of("support"),
                List.of("退款制度"),
                "POLICY",
                "v1",
                content
        );
    }
}
