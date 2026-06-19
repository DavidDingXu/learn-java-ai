package com.xiaoding.javaai.observability;

import com.xiaoding.javaai.observability.service.AiSpan;
import com.xiaoding.javaai.observability.service.AiTrace;
import com.xiaoding.javaai.observability.service.AiTraceRecorder;
import com.xiaoding.javaai.observability.service.CostSummary;
import com.xiaoding.javaai.observability.service.SpanType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiTraceStructuredEventsTest {

    @Test
    void recordsPromptRagToolAndAgentAsTypedSpans() {
        AiTraceRecorder recorder = new AiTraceRecorder();
        AiTrace trace = recorder.startTrace("u1001", "ticket-advice");

        recorder.recordPrompt(trace.traceId(), "ticket-advice-v3", List.of("ticketId", "operatorRole"));
        recorder.recordRag(trace.traceId(), "发货后退款", List.of("refund-policy-001-c1"), List.of("0.92"));
        recorder.recordTool(trace.traceId(), "order.lookup", "orderId=O-1001", "FOUND");
        recorder.recordAgentStep(trace.traceId(), "risk.review", "high amount refund", "require approval");

        List<AiSpan> spans = recorder.findSpans(trace.traceId());

        assertThat(spans).extracting(AiSpan::type)
                .containsExactly(SpanType.PROMPT, SpanType.RAG, SpanType.TOOL, SpanType.AGENT);
        assertThat(spans.get(0).attributes())
                .containsEntry("templateVersion", "ticket-advice-v3")
                .containsEntry("variables", List.of("ticketId", "operatorRole"));
        assertThat(spans.get(1).attributes())
                .containsEntry("query", "发货后退款")
                .containsEntry("chunkIds", List.of("refund-policy-001-c1"));
    }

    @Test
    void summarizesCostByScenarioAcrossTraces() {
        AiTraceRecorder recorder = new AiTraceRecorder();
        AiTrace ticketTrace = recorder.startTrace("u1001", "ticket-advice");
        AiTrace policyTrace = recorder.startTrace("u1002", "policy-qa");

        recorder.recordModelUsage(ticketTrace.traceId(), "gpt-4o-mini", 100, 200, 0.0003);
        recorder.recordModelUsage(ticketTrace.traceId(), "gpt-4o-mini", 50, 80, 0.00013);
        recorder.recordModelUsage(policyTrace.traceId(), "gpt-4o-mini", 30, 40, 0.00007);

        CostSummary summary = recorder.costByScenario().get("ticket-advice");

        assertThat(summary.totalCost()).isEqualByComparingTo(BigDecimal.valueOf(0.00043));
        assertThat(summary.inputTokens()).isEqualTo(150);
        assertThat(summary.outputTokens()).isEqualTo(280);
        assertThat(summary.callCount()).isEqualTo(2);
    }
}
