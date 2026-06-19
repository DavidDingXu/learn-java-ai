package com.xiaoding.javaai.rag;

import com.xiaoding.javaai.rag.service.DocumentChunk;
import com.xiaoding.javaai.rag.service.HybridRetrievalResult;
import com.xiaoding.javaai.rag.service.OperatorScope;
import com.xiaoding.javaai.rag.service.RerankService;
import com.xiaoding.javaai.rag.service.RerankedRetrievalResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RerankServiceTest {

    private final RerankService rerankService = new RerankService();

    @Test
    void movesTheMostRelevantCandidateAheadOfHigherOriginalScore() {
        HybridRetrievalResult genericRefund = result(
                chunk("generic-refund", "support", List.of("退款制度"), "退款错误码 RF-102 需要人工复核。"),
                0.060,
                Set.of("keyword")
        );
        HybridRetrievalResult shippedRefund = result(
                chunk("shipped-refund", "support", List.of("退款制度", "发货后退款"), "客户已发货后申请退款，需要先确认物流状态，再判断是否拦截或拒收。"),
                0.030,
                Set.of("keyword", "vector")
        );

        List<RerankedRetrievalResult> results = rerankService.rerank(
                "发货后退款怎么处理",
                List.of(genericRefund, shippedRefund),
                new OperatorScope("tenant-a", "support"),
                2
        );

        assertThat(results).extracting(result -> result.chunk().chunkId())
                .containsExactly("shipped-refund", "generic-refund");
        assertThat(results.get(0).rerankScore()).isGreaterThan(results.get(1).rerankScore());
        assertThat(results.get(0).sources()).containsExactlyInAnyOrder("keyword", "vector");
        assertThat(results.get(0).reasons()).contains("heading", "content", "multi-source");
    }

    @Test
    void filtersCandidatesOutsideOperatorScopeBeforeRanking() {
        HybridRetrievalResult supportRefund = result(
                chunk("support-refund", "support", List.of("退款制度"), "退款需要核对订单和物流状态。"),
                0.010,
                Set.of("keyword")
        );
        HybridRetrievalResult financeRefund = result(
                chunk("finance-refund", "finance", List.of("财务制度"), "财务退款审批规则，命中发货后退款处理。"),
                0.090,
                Set.of("keyword", "vector")
        );

        List<RerankedRetrievalResult> results = rerankService.rerank(
                "发货后退款怎么处理",
                List.of(financeRefund, supportRefund),
                new OperatorScope("tenant-a", "support"),
                5
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).chunk().chunkId()).isEqualTo("support-refund");
    }

    @Test
    void keepsOriginalCandidateEvidenceForTraceAndDebugging() {
        HybridRetrievalResult candidate = result(
                chunk("refund-trace", "support", List.of("退款制度"), "退款处理需要记录依据。"),
                0.045,
                Set.of("vector")
        );

        List<RerankedRetrievalResult> results = rerankService.rerank(
                "退款处理",
                List.of(candidate),
                new OperatorScope("tenant-a", "support"),
                1
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).originalScore()).isEqualTo(0.045);
        assertThat(results.get(0).sources()).containsExactly("vector");
        assertThat(results.get(0).chunk()).isEqualTo(candidate.chunk());
    }

    private HybridRetrievalResult result(DocumentChunk chunk, double score, Set<String> sources) {
        return new HybridRetrievalResult(chunk, score, sources);
    }

    private DocumentChunk chunk(String chunkId, String department, List<String> headingPath, String content) {
        return new DocumentChunk(
                "refund-policy-001",
                chunkId,
                "tenant-a",
                Set.of(department),
                headingPath,
                "POLICY",
                "v1",
                content
        );
    }
}
