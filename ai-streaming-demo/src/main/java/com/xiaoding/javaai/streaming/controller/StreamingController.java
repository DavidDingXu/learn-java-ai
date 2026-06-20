package com.xiaoding.javaai.streaming.controller;

import com.xiaoding.javaai.streaming.service.StreamEvent;
import com.xiaoding.javaai.streaming.service.StreamSessionService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/stream")
public class StreamingController {

    private final StreamSessionService streamSessionService;

    public StreamingController(StreamSessionService streamSessionService) {
        this.streamSessionService = streamSessionService;
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

    private record StreamCursor(String sessionId, String lastEventId) {
    }
}
