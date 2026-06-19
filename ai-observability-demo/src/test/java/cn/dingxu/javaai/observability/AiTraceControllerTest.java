package cn.dingxu.javaai.observability;

import cn.dingxu.javaai.observability.controller.AiTraceController;
import cn.dingxu.javaai.observability.service.AiTraceRecorder;
import cn.dingxu.javaai.observability.service.FeedbackStore;
import cn.dingxu.javaai.observability.service.QuotaService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiTraceControllerTest {

    private final AiTraceController controller = new AiTraceController(
            new AiTraceRecorder(),
            new QuotaService(),
            new FeedbackStore()
    );

    @Test
    void exposesStructuredTraceEndpoints() {
        var trace = controller.start(new AiTraceController.StartTraceRequest("u1001", "ticket-advice"));

        controller.recordPrompt(trace.traceId(), new AiTraceController.RecordPromptRequest(
                "ticket-advice-v3", List.of("ticketId", "operatorRole")));
        controller.recordRag(trace.traceId(), new AiTraceController.RecordRagRequest(
                "发货后退款", List.of("refund-policy-001-c1"), List.of("0.92")));
        controller.recordTool(trace.traceId(), new AiTraceController.RecordToolRequest(
                "order.lookup", "orderId=O-1001", "FOUND"));
        controller.recordAgentStep(trace.traceId(), new AiTraceController.RecordAgentStepRequest(
                "risk.review", "high amount refund", "require approval"));

        assertThat(controller.snapshot(trace.traceId()).spans()).hasSize(4);
    }

    @Test
    void exposesTraceEventEndpoint() {
        var trace = controller.start(new AiTraceController.StartTraceRequest("u1001", "ticket-advice"));

        controller.recordEvent(trace.traceId(), new AiTraceController.RecordEventRequest(
                "stream.first-token", java.util.Map.of("ttftMs", 320)));

        assertThat(controller.snapshot(trace.traceId()).events()).hasSize(1);
    }

    @Test
    void exposesQuotaAndFeedbackEndpoints() {
        var quota = controller.checkQuota(new AiTraceController.QuotaRequest("tenant-a", "u1001", 500));
        var feedback = controller.recordFeedback(new AiTraceController.FeedbackRequest(
                "trace-1", "ticket-advice", "bad", "引用了错误制度"));
        var report = controller.qualityReport("ticket-advice");

        assertThat(quota.allowed()).isTrue();
        assertThat(feedback.bad()).isTrue();
        assertThat(report.bad()).isEqualTo(1);
    }
}
