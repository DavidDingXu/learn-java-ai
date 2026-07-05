package com.xiaoding.javaai.observability.controller;

import com.xiaoding.javaai.common.ai.LiveAiResult;
import com.xiaoding.javaai.common.ai.SpringAiChatCaller;
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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/traces")
public class AiTraceController {

    private final AiTraceRecorder recorder;
    private final QuotaService quotaService;
    private final FeedbackStore feedbackStore;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final String apiKey;
    private final String modelName;

    public AiTraceController(AiTraceRecorder recorder, QuotaService quotaService, FeedbackStore feedbackStore) {
        this(recorder, quotaService, feedbackStore, new EmptyChatClientBuilderProvider(), "demo-key", "gpt-4o-mini");
    }

    @Autowired
    public AiTraceController(AiTraceRecorder recorder,
                             QuotaService quotaService,
                             FeedbackStore feedbackStore,
                             ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                             @Value("${spring.ai.openai.api-key:}") String apiKey,
                             @Value("${java-ai.observability.model-name:gpt-4o-mini}") String modelName) {
        this.recorder = recorder;
        this.quotaService = quotaService;
        this.feedbackStore = feedbackStore;
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.apiKey = apiKey;
        this.modelName = modelName;
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

    @PostMapping("/live-model-call")
    public LiveTraceResponse liveModelCall(@RequestBody LiveTraceRequest request) {
        AiTrace trace = recorder.startTrace(request.userId(), request.scenario());
        recorder.recordPrompt(trace.traceId(), request.promptVersion(), List.of("question"));
        LiveAiResult result = new SpringAiChatCaller(
                chatClientBuilderProvider.getIfAvailable(),
                apiKey,
                modelName,
                "ai-observability-demo"
        ).call("你是企业工单系统里的 AI 助手。回答要短，并说明依据边界。", request.question());
        int inputTokens = estimateTokens(request.question());
        int outputTokens = estimateTokens(result.content());
        recorder.recordModelUsage(trace.traceId(), result.model(), inputTokens, outputTokens, 0);
        recorder.recordEvent(trace.traceId(), "model.call.completed", Map.of(
                "model", result.model(),
                "inputTokens", inputTokens,
                "outputTokens", outputTokens
        ));
        return new LiveTraceResponse(result, recorder.snapshot(trace.traceId()));
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AiConfigurationError aiConfigurationError(IllegalStateException error) {
        return new AiConfigurationError("AI_CONFIGURATION_REQUIRED", error.getMessage());
    }

    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, text.length() / 2);
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

    public record LiveTraceRequest(String userId, String scenario, String promptVersion, String question) {
    }

    public record LiveTraceResponse(LiveAiResult modelResult, AiTraceSnapshot trace) {
    }

    public record AiConfigurationError(String code, String message) {
    }

    private static final class EmptyChatClientBuilderProvider implements ObjectProvider<ChatClient.Builder> {
        @Override
        public ChatClient.Builder getObject(Object... args) throws BeansException {
            return null;
        }

        @Override
        public ChatClient.Builder getIfAvailable() throws BeansException {
            return null;
        }

        @Override
        public ChatClient.Builder getIfUnique() throws BeansException {
            return null;
        }

        @Override
        public ChatClient.Builder getObject() throws BeansException {
            return null;
        }
    }
}
