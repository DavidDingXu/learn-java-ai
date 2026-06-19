package com.xiaoding.javaai.eval;

import com.xiaoding.javaai.eval.service.RagEvalCase;
import com.xiaoding.javaai.eval.service.RagEvalObservation;
import com.xiaoding.javaai.eval.service.RagEvalReport;
import com.xiaoding.javaai.eval.service.RagEvalRunner;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RagEvalRunnerTest {

    private final RagEvalRunner runner = new RagEvalRunner();

    @Test
    void evaluatesRetrievalAndCitationSeparately() {
        RagEvalCase evalCase = new RagEvalCase(
                "rag-1",
                "发货后退款怎么处理",
                Set.of("refund-policy-001-c1"),
                false
        );
        RagEvalObservation observation = new RagEvalObservation(
                "rag-1",
                List.of("refund-policy-001-c1", "refund-policy-002-c1"),
                List.of("refund-policy-001-c1"),
                false,
                "根据制度，发货后退款需先核对物流状态。"
        );

        RagEvalReport report = runner.run(List.of(evalCase), List.of(observation));

        assertThat(report.total()).isEqualTo(1);
        assertThat(report.retrievalPassed()).isEqualTo(1);
        assertThat(report.citationPassed()).isEqualTo(1);
        assertThat(report.noEvidencePassed()).isZero();
        assertThat(report.results().get(0).passed()).isTrue();
    }

    @Test
    void failsCitationWhenAnswerDoesNotCiteExpectedEvidence() {
        RagEvalCase evalCase = new RagEvalCase(
                "rag-2",
                "高金额退款能不能自动处理",
                Set.of("refund-policy-002-c1"),
                false
        );
        RagEvalObservation observation = new RagEvalObservation(
                "rag-2",
                List.of("refund-policy-002-c1"),
                List.of("refund-policy-001-c1"),
                false,
                "高金额退款必须转人工复核。"
        );

        RagEvalReport report = runner.run(List.of(evalCase), List.of(observation));

        assertThat(report.retrievalPassed()).isEqualTo(1);
        assertThat(report.citationPassed()).isZero();
        assertThat(report.results().get(0).passed()).isFalse();
        assertThat(report.results().get(0).reason()).contains("citation_miss");
    }

    @Test
    void evaluatesNoEvidenceFallback() {
        RagEvalCase evalCase = new RagEvalCase(
                "rag-3",
                "薪资制度怎么查",
                Set.of(),
                true
        );
        RagEvalObservation observation = new RagEvalObservation(
                "rag-3",
                List.of(),
                List.of(),
                true,
                "没有检索到当前用户可访问的依据，不能生成处理建议。"
        );

        RagEvalReport report = runner.run(List.of(evalCase), List.of(observation));

        assertThat(report.total()).isEqualTo(1);
        assertThat(report.noEvidencePassed()).isEqualTo(1);
        assertThat(report.results().get(0).passed()).isTrue();
    }
}
