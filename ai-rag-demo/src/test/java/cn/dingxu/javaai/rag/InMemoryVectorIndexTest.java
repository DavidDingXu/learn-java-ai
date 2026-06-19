package cn.dingxu.javaai.rag;

import cn.dingxu.javaai.rag.service.DocumentChunk;
import cn.dingxu.javaai.rag.service.InMemoryVectorIndex;
import cn.dingxu.javaai.rag.service.OperatorScope;
import cn.dingxu.javaai.rag.service.VectorIndexEntry;
import cn.dingxu.javaai.rag.service.VectorSearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryVectorIndexTest {

    @Test
    void searchesByCosineSimilarityAndKeepsTopKOrder() {
        InMemoryVectorIndex index = new InMemoryVectorIndex();
        index.upsert(List.of(
                entry(chunk("refund-c1", "support", "已发货退款需先核对物流状态。"), List.of(1.0, 0.0)),
                entry(chunk("refund-c2", "support", "高金额退款必须转人工复核。"), List.of(0.8, 0.2)),
                entry(chunk("refund-c3", "support", "客服排班制度。"), List.of(0.0, 1.0))
        ));

        List<VectorSearchResult> results = index.search(
                List.of(1.0, 0.0),
                new OperatorScope("tenant-a", "support"),
                2
        );

        assertThat(results).hasSize(2);
        assertThat(results).extracting(result -> result.chunk().chunkId())
                .containsExactly("refund-c1", "refund-c2");
        assertThat(results.get(0).score()).isGreaterThan(results.get(1).score());
    }

    @Test
    void filtersByTenantAndDepartmentBeforeReturningVectorResults() {
        InMemoryVectorIndex index = new InMemoryVectorIndex();
        index.upsert(List.of(
                entry(chunk("support-c1", "support", "退款制度。"), List.of(1.0, 0.0)),
                entry(chunk("hr-c1", "hr", "薪资制度。"), List.of(1.0, 0.0)),
                entry(new DocumentChunk("other-doc", "other-c1", "tenant-b", Set.of("support"), "其他租户退款制度。"),
                        List.of(1.0, 0.0))
        ));

        List<VectorSearchResult> results = index.search(
                List.of(1.0, 0.0),
                new OperatorScope("tenant-a", "support"),
                5
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).chunk().chunkId()).isEqualTo("support-c1");
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
