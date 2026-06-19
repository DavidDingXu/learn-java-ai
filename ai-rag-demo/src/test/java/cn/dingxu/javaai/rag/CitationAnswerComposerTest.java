package cn.dingxu.javaai.rag;

import cn.dingxu.javaai.rag.service.CitationAnswerComposer;
import cn.dingxu.javaai.rag.service.DocumentChunk;
import cn.dingxu.javaai.rag.service.RagAnswer;
import cn.dingxu.javaai.rag.service.RerankedRetrievalResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CitationAnswerComposerTest {

    private final CitationAnswerComposer composer = new CitationAnswerComposer();

    @Test
    void composesAnswerOnlyFromSelectedEvidenceAndCitations() {
        DocumentChunk shippedRefund = chunk(
                "refund-policy-001",
                "c1",
                "发货后退款需先核对物流状态，确认客户是否拒收、退回或已签收。"
        );
        DocumentChunk highRiskRefund = chunk(
                "refund-policy-002",
                "c2",
                "高金额退款必须转人工复核，系统只能生成建议，不能直接执行退款。"
        );

        RagAnswer answer = composer.compose(
                "客户发货后申请退款怎么处理？",
                List.of(result(shippedRefund, 0.82), result(highRiskRefund, 0.63)),
                2
        );

        assertThat(answer.content()).contains("发货后退款需先核对物流状态");
        assertThat(answer.content()).contains("高金额退款必须转人工复核");
        assertThat(answer.content()).doesNotContain("可以直接退款");
        assertThat(answer.citations()).hasSize(2);
        assertThat(answer.citations()).extracting("documentId")
                .containsExactly("refund-policy-001", "refund-policy-002");
        assertThat(answer.citations()).extracting("chunkId")
                .containsExactly("c1", "c2");
    }

    @Test
    void returnsNoEvidenceAnswerWhenSelectedEvidenceIsEmpty() {
        RagAnswer answer = composer.compose(
                "客户发货后申请退款怎么处理？",
                List.of(),
                3
        );

        assertThat(answer.content()).isEqualTo("没有检索到当前用户可访问的依据，不能生成处理建议。");
        assertThat(answer.citations()).isEmpty();
    }

    @Test
    void limitsEvidenceCountBeforeComposingAnswer() {
        DocumentChunk first = chunk("refund-policy-001", "c1", "第一条制度依据。");
        DocumentChunk second = chunk("refund-policy-002", "c2", "第二条制度依据。");
        DocumentChunk third = chunk("refund-policy-003", "c3", "第三条不应该进入本次答案。");

        RagAnswer answer = composer.compose(
                "退款怎么处理？",
                List.of(result(first, 0.9), result(second, 0.8), result(third, 0.7)),
                2
        );

        assertThat(answer.content()).contains("第一条制度依据");
        assertThat(answer.content()).contains("第二条制度依据");
        assertThat(answer.content()).doesNotContain("第三条不应该进入本次答案");
        assertThat(answer.citations()).extracting("chunkId").containsExactly("c1", "c2");
    }

    private RerankedRetrievalResult result(DocumentChunk chunk, double rerankScore) {
        return new RerankedRetrievalResult(
                chunk,
                rerankScore / 2,
                rerankScore,
                Set.of("keyword", "vector"),
                List.of("content")
        );
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
