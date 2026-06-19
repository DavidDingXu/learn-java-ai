package cn.dingxu.javaai.observability.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiTraceRecorder {

    private final Map<String, AiTrace> traces = new LinkedHashMap<>();
    private final Map<String, List<AiSpan>> spansByTraceId = new LinkedHashMap<>();
    private final Map<String, List<AiEvent>> eventsByTraceId = new LinkedHashMap<>();
    private final Map<String, List<ModelUsage>> usageByTraceId = new LinkedHashMap<>();

    public synchronized AiTrace startTrace(String userId, String scenario) {
        AiTrace trace = new AiTrace(UUID.randomUUID().toString(), userId, scenario, Instant.now());
        traces.put(trace.traceId(), trace);
        spansByTraceId.put(trace.traceId(), new ArrayList<>());
        eventsByTraceId.put(trace.traceId(), new ArrayList<>());
        usageByTraceId.put(trace.traceId(), new ArrayList<>());
        return trace;
    }

    public synchronized AiSpan recordSpan(String traceId, String name, SpanType type, Map<String, Object> attributes) {
        ensureTraceExists(traceId);
        Instant now = Instant.now();
        AiSpan span = new AiSpan(UUID.randomUUID().toString(), traceId, name, type, now, now, attributes);
        spansByTraceId.computeIfAbsent(traceId, ignored -> new ArrayList<>()).add(span);
        return span;
    }

    public synchronized AiSpan recordPrompt(String traceId, String templateVersion, List<String> variables) {
        return recordSpan(traceId, "prompt.render", SpanType.PROMPT, Map.of(
                "templateVersion", templateVersion,
                "variables", variables == null ? List.of() : List.copyOf(variables)
        ));
    }

    public synchronized AiSpan recordRag(String traceId, String query, List<String> chunkIds, List<String> scores) {
        return recordSpan(traceId, "rag.retrieve", SpanType.RAG, Map.of(
                "query", query,
                "chunkIds", chunkIds == null ? List.of() : List.copyOf(chunkIds),
                "scores", scores == null ? List.of() : List.copyOf(scores)
        ));
    }

    public synchronized AiSpan recordTool(String traceId, String toolName, String argsDigest, String resultStatus) {
        return recordSpan(traceId, "tool.call", SpanType.TOOL, Map.of(
                "toolName", toolName,
                "argsDigest", argsDigest,
                "resultStatus", resultStatus
        ));
    }

    public synchronized AiSpan recordAgentStep(String traceId, String stepName, String observation, String decision) {
        return recordSpan(traceId, "agent.step", SpanType.AGENT, Map.of(
                "stepName", stepName,
                "observation", observation,
                "decision", decision
        ));
    }

    public synchronized AiEvent recordEvent(String traceId, String name, Map<String, Object> attributes) {
        ensureTraceExists(traceId);
        AiEvent event = new AiEvent(UUID.randomUUID().toString(), traceId, name, Instant.now(), attributes);
        eventsByTraceId.computeIfAbsent(traceId, ignored -> new ArrayList<>()).add(event);
        return event;
    }

    public synchronized void recordModelUsage(String traceId,
                                              String model,
                                              int inputTokens,
                                              int outputTokens,
                                              double cost) {
        ensureTraceExists(traceId);
        usageByTraceId.computeIfAbsent(traceId, ignored -> new ArrayList<>())
                .add(new ModelUsage(traceId, model, inputTokens, outputTokens, BigDecimal.valueOf(cost), Instant.now()));
    }

    public synchronized List<AiSpan> findSpans(String traceId) {
        return List.copyOf(spansByTraceId.getOrDefault(traceId, List.of()));
    }

    public synchronized List<AiEvent> findEvents(String traceId) {
        return List.copyOf(eventsByTraceId.getOrDefault(traceId, List.of()));
    }

    public synchronized BigDecimal costOf(String traceId) {
        return usageByTraceId.getOrDefault(traceId, List.of()).stream()
                .map(ModelUsage::cost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public synchronized Map<String, CostSummary> costByScenario() {
        Map<String, CostSummary> summaryByScenario = new LinkedHashMap<>();
        for (Map.Entry<String, List<ModelUsage>> entry : usageByTraceId.entrySet()) {
            AiTrace trace = traces.get(entry.getKey());
            if (trace == null) {
                continue;
            }
            CostSummary current = summaryByScenario.getOrDefault(trace.scenario(), CostSummary.empty(trace.scenario()));
            for (ModelUsage usage : entry.getValue()) {
                current = current.add(usage);
            }
            summaryByScenario.put(trace.scenario(), current);
        }
        return Map.copyOf(summaryByScenario);
    }

    public synchronized AiTraceSnapshot snapshot(String traceId) {
        ensureTraceExists(traceId);
        return new AiTraceSnapshot(traces.get(traceId), findSpans(traceId), findEvents(traceId), costOf(traceId));
    }

    private void ensureTraceExists(String traceId) {
        if (!traces.containsKey(traceId)) {
            throw new IllegalArgumentException("trace not found: " + traceId);
        }
    }
}
