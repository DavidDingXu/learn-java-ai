package com.xiaoding.javaai.streaming.controller;

import com.xiaoding.javaai.common.ai.SpringAiChatCaller;
import com.xiaoding.javaai.streaming.service.StreamEvent;
import com.xiaoding.javaai.streaming.service.StreamSessionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/stream")
public class StreamingController {

    private final StreamSessionService streamSessionService;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final String apiKey;
    private final String modelName;

    public StreamingController(StreamSessionService streamSessionService,
                               ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                               @Value("${spring.ai.openai.api-key:}") String apiKey,
                               @Value("${java-ai.streaming.model-name:gpt-4o-mini}") String modelName) {
        this.streamSessionService = streamSessionService;
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    @GetMapping(value = "/ticket-advice", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamTicketAdvice(
            @RequestParam(value = "sessionId", defaultValue = "s1001") String sessionId,
            @RequestParam(value = "lastEventId", required = false) String lastEventId,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventIdHeader) {

        StreamCursor cursor = resolveCursor(sessionId, lastEventId, lastEventIdHeader);
        List<StreamEvent> events = streamSessionService.createEvents(cursor.sessionId(), "先核对订单，再检索退款制度。");
        if (cursor.lastEventId() != null) {
            events = streamSessionService.resumeAfter(events, cursor.lastEventId());
        }

        return Flux.fromIterable(events)
                .delayElements(Duration.ofMillis(120))
                .map(event -> ServerSentEvent.<String>builder()
                        .id(event.eventId())
                        .event(event.type())
                        .data(event.data())
                        .build());
    }

    @GetMapping(value = "/ticket-advice/live", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamLiveTicketAdvice(
            @RequestParam(value = "question", defaultValue = "客户申请退款但订单已经发货，应该怎么处理？") String question) {
        SpringAiChatCaller caller = new SpringAiChatCaller(
                chatClientBuilderProvider.getIfAvailable(),
                apiKey,
                modelName,
                "ai-streaming-demo"
        );
        return caller.stream(
                        "你是企业工单系统里的 AI 助手。用中文分段输出，必须说明依据、风险和下一步动作。",
                        question)
                .index()
                .map(tuple -> ServerSentEvent.<String>builder()
                        .id("live-" + (tuple.getT1() + 1))
                        .event("model-token")
                        .data(tuple.getT2())
                        .build())
                .concatWithValues(ServerSentEvent.<String>builder()
                        .id("live-done")
                        .event("done")
                        .data("[DONE]")
                        .build());
    }

    private StreamCursor resolveCursor(String sessionId, String lastEventId, String lastEventIdHeader) {
        String effectiveSessionId = hasText(sessionId) ? sessionId.trim() : "s1001";
        String effectiveLastEventId = firstText(lastEventId, lastEventIdHeader);
        if (effectiveLastEventId == null) {
            int suffixIndex = effectiveSessionId.lastIndexOf('-');
            if (suffixIndex > 0 && suffixIndex < effectiveSessionId.length() - 1
                    && effectiveSessionId.substring(suffixIndex + 1).chars().allMatch(Character::isDigit)) {
                effectiveLastEventId = effectiveSessionId;
                effectiveSessionId = effectiveSessionId.substring(0, suffixIndex);
            }
        }
        return new StreamCursor(effectiveSessionId, effectiveLastEventId);
    }

    private String firstText(String first, String second) {
        if (hasText(first)) {
            return first.trim();
        }
        if (hasText(second)) {
            return second.trim();
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public AiConfigurationError aiConfigurationError(IllegalStateException error) {
        return new AiConfigurationError("AI_CONFIGURATION_REQUIRED", error.getMessage());
    }

    private record StreamCursor(String sessionId, String lastEventId) {
    }

    public record AiConfigurationError(String code, String message) {
    }
}
