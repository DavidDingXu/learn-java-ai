package com.xiaoding.javaai.observability.controller;

import com.xiaoding.javaai.observability.service.AiSpan;
import com.xiaoding.javaai.observability.service.AiEvent;
import com.xiaoding.javaai.observability.service.AiTrace;
import com.xiaoding.javaai.observability.service.AiTraceRecorder;
import com.xiaoding.javaai.observability.service.AiTraceSnapshot;
import com.xiaoding.javaai.observability.service.CostSummary;
import com.xiaoding.javaai.observability.service.FeedbackRecord;
import com.xiaoding.javaai.observability.service.FeedbackStore;
import com.xiaoding.javaai.observability.service.QualityReport;
import com.xiaoding.javaai.observability.service.QuotaDecision;
import com.xiaoding.javaai.observability.service.QuotaService;
import com.xiaoding.javaai.observability.service.SpanType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/traces")
public class AiTraceController {

    private final AiTraceRecorder recorder;
    private final QuotaService quotaService;
    private final FeedbackStore feedbackStore;

    public AiTraceController(AiTraceRecorder recorder, QuotaService quotaService, FeedbackStore feedbackStore) {
        this.recorder = recorder;
        this.quotaService = quotaService;
        this.feedbackStore = feedbackStore;
    }

    @PostMapping
    public AiTrace start(@RequestBody StartTraceRequest request) {
        return recorder.startTrace(request.userId(), request.scenario());
    }

    @PostMapping("/{traceId}/spans")
    public AiSpan recordSpan(@PathVariable("traceId") String traceId, @RequestBody RecordSpanRequest request) {
        return recorder.recordSpan(traceId, request.name(), request.type(), request.attributes());
    }

    @PostMapping("/{traceId}/prompt")
    public AiSpan recordPrompt(@PathVariable("traceId") String traceId, @RequestBody RecordPromptRequest request) {
        return recorder.recordPrompt(traceId, request.templateVersion(), request.variables());
    }

    @PostMapping("/{traceId}/rag")
    public AiSpan recordRag(@PathVariable("traceId") String traceId, @RequestBody RecordRagRequest request) {
        return recorder.recordRag(traceId, request.query(), request.chunkIds(), request.scores());
    }

    @PostMapping("/{traceId}/tool")
    public AiSpan recordTool(@PathVariable("traceId") String traceId, @RequestBody RecordToolRequest request) {
        return recorder.recordTool(traceId, request.toolName(), request.argsDigest(), request.resultStatus());
    }

    @PostMapping("/{traceId}/agent-step")
    public AiSpan recordAgentStep(@PathVariable("traceId") String traceId, @RequestBody RecordAgentStepRequest request) {
        return recorder.recordAgentStep(traceId, request.stepName(), request.observation(), request.decision());
    }

    @PostMapping("/{traceId}/events")
    public AiEvent recordEvent(@PathVariable("traceId") String traceId, @RequestBody RecordEventRequest request) {
        return recorder.recordEvent(traceId, request.name(), request.attributes());
    }

    @PostMapping("/{traceId}/model-usage")
    public void recordModelUsage(@PathVariable("traceId") String traceId, @RequestBody RecordModelUsageRequest request) {
        recorder.recordModelUsage(traceId, request.model(), request.inputTokens(), request.outputTokens(), request.cost());
    }

    @GetMapping("/{traceId}")
    public AiTraceSnapshot snapshot(@PathVariable("traceId") String traceId) {
        return recorder.snapshot(traceId);
    }

    @GetMapping("/cost/by-scenario")
    public Map<String, CostSummary> costByScenario() {
        return recorder.costByScenario();
    }

    @PostMapping("/quota/check")
    public QuotaDecision checkQuota(@RequestBody QuotaRequest request) {
        return quotaService.checkAndConsume(request.tenantId(), request.userId(), request.requestedTokens());
    }

    @PostMapping("/feedback")
    public FeedbackRecord recordFeedback(@RequestBody FeedbackRequest request) {
        return feedbackStore.record(request.traceId(), request.scenario(), request.rating(), request.reason());
    }

    @GetMapping("/quality/{scenario}")
    public QualityReport qualityReport(@PathVariable("scenario") String scenario) {
        return feedbackStore.reportByScenario(scenario);
    }

    public record StartTraceRequest(String userId, String scenario) {
    }

    public record RecordSpanRequest(String name, SpanType type, Map<String, Object> attributes) {
    }

    public record RecordPromptRequest(String templateVersion, List<String> variables) {
    }

    public record RecordRagRequest(String query, List<String> chunkIds, List<String> scores) {
    }

    public record RecordToolRequest(String toolName, String argsDigest, String resultStatus) {
    }

    public record RecordAgentStepRequest(String stepName, String observation, String decision) {
    }

    public record RecordEventRequest(String name, Map<String, Object> attributes) {
    }

    public record RecordModelUsageRequest(String model, int inputTokens, int outputTokens, double cost) {
    }

    public record QuotaRequest(String tenantId, String userId, int requestedTokens) {
    }

    public record FeedbackRequest(String traceId, String scenario, String rating, String reason) {
    }
}
