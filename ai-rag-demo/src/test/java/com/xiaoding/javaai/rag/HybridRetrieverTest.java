package com.xiaoding.javaai.rag;

import com.xiaoding.javaai.rag.service.DocumentChunk;
import com.xiaoding.javaai.rag.service.HybridRetrievalResult;
import com.xiaoding.javaai.rag.service.HybridRetriever;
import com.xiaoding.javaai.rag.service.InMemoryVectorIndex;
import com.xiaoding.javaai.rag.service.OperatorScope;
import com.xiaoding.javaai.rag.service.VectorIndexEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HybridRetrieverTest {

    @Test
    void mergesKeywordAndVectorResultsWithRrfScores() {
        DocumentChunk keywordOnly = chunk("refund-keyword", "support", "退款错误码 RF-102 需要人工复核。");
        DocumentChunk vectorOnly = chunk("refund-vector", "support", "客户已签收时要先确认物流状态。");
        DocumentChunk both = chunk("refund-both", "support", "退款需要核对订单、物流和金额。");
        InMemoryVectorIndex vectorIndex = new InMemoryVectorIndex();
        vectorIndex.upsert(List.of(
                entry(vectorOnly, List.of(1.0, 0.0)),
                entry(both, List.of(0.9, 0.1))
        ));
        HybridRetriever retriever = new HybridRetriever(List.of(keywordOnly, vectorOnly, both), vectorIndex);

        List<HybridRetrievalResult> results = retriever.search(
                "退款 RF-102",
                List.of(1.0, 0.0),
                new OperatorScope("tenant-a", "support"),
                3
        );

        assertThat(results).hasSize(3);
        assertThat(results).extracting(result -> result.chunk().chunkId())
                .contains("refund-keyword", "refund-vector", "refund-both");
        assertThat(results.stream()
                .filter(result -> result.chunk().chunkId().equals("refund-both"))
                .findFirst()
                .orElseThrow()
                .sources()).containsExactlyInAnyOrder("keyword", "vector");
    }

    @Test
    void filtersUnauthorizedChunksBeforeHybridMerge() {
        DocumentChunk supportChunk = chunk("support-refund", "support", "退款制度。");
        DocumentChunk hrChunk = chunk("hr-refund", "hr", "HR 内部退款补贴说明。");
        InMemoryVectorIndex vectorIndex = new InMemoryVectorIndex();
        vectorIndex.upsert(List.of(
                entry(supportChunk, List.of(1.0, 0.0)),
                entry(hrChunk, List.of(1.0, 0.0))
        ));
        HybridRetriever retriever = new HybridRetriever(List.of(supportChunk, hrChunk), vectorIndex);

        List<HybridRetrievalResult> results = retriever.search(
                "退款",
                List.of(1.0, 0.0),
                new OperatorScope("tenant-a", "support"),
                5
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).chunk().chunkId()).isEqualTo("support-refund");
    }

    private VectorIndexEntry entry(DocumentChunk chunk, List<Double> vector) {
        return new VectorIndexEntry(chunk, vector);
    }

    private DocumentChunk chunk(String chunkId, String department, String content) {
        return new DocumentChunk(
                "refund-policy-001",
                chunkId,
                "tenant-a",
                Set.of(department),
                List.of("退款制度"),
                "POLICY",
                "v1",
                content
        );
    }
}
