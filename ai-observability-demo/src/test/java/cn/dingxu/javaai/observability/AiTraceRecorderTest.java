package cn.dingxu.javaai.observability;

import cn.dingxu.javaai.observability.service.AiSpan;
import cn.dingxu.javaai.observability.service.AiEvent;
import cn.dingxu.javaai.observability.service.AiTrace;
import cn.dingxu.javaai.observability.service.AiTraceRecorder;
import cn.dingxu.javaai.observability.service.SpanType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiTraceRecorderTest {

    @Test
    void recordsPromptRagModelAndToolSpansForOneTrace() {
        AiTraceRecorder recorder = new AiTraceRecorder();
        AiTrace trace = recorder.startTrace("u1001", "ticket-advice");

        recorder.recordSpan(trace.traceId(), "prompt.render", SpanType.PROMPT, Map.of("version", "v1"));
        recorder.recordSpan(trace.traceId(), "rag.retrieve", SpanType.RAG, Map.of("chunks", 2));
        recorder.recordSpan(trace.traceId(), "model.call", SpanType.MODEL, Map.of("model", "gpt-4o-mini", "tokens", 320));
        recorder.recordSpan(trace.traceId(), "tool.lookup", SpanType.TOOL, Map.of("tool", "ticket.lookup"));

        assertThat(recorder.findSpans(trace.traceId()))
                .extracting(AiSpan::type)
                .containsExactly(SpanType.PROMPT, SpanType.RAG, SpanType.MODEL, SpanType.TOOL);
    }

    @Test
    void calculatesTokenCostByTrace() {
        AiTraceRecorder recorder = new AiTraceRecorder();
        AiTrace trace = recorder.startTrace("u1001", "ticket-advice");

        recorder.recordModelUsage(trace.traceId(), "gpt-4o-mini", 100, 200, 0.0003);

        assertThat(recorder.costOf(trace.traceId())).isEqualByComparingTo("0.0003");
    }

    @Test
    void recordsEventsForTraceSnapshot() {
        AiTraceRecorder recorder = new AiTraceRecorder();
        AiTrace trace = recorder.startTrace("u1001", "ticket-advice");

        recorder.recordEvent(trace.traceId(), "stream.first-token", Map.of("ttftMs", 320));
        recorder.recordEvent(trace.traceId(), "user.feedback", Map.of("rating", "bad"));

        assertThat(recorder.snapshot(trace.traceId()).events())
                .extracting(AiEvent::name)
                .containsExactly("stream.first-token", "user.feedback");
    }
}
