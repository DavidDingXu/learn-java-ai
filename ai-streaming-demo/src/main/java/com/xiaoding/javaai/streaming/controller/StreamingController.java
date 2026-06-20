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
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {

        List<StreamEvent> events = streamSessionService.createEvents(sessionId, "先核对订单，再检索退款制度。");
        if (lastEventId != null && !lastEventId.isBlank()) {
            events = streamSessionService.resumeAfter(events, lastEventId);
        }

        return Flux.fromIterable(events)
                .delayElements(Duration.ofMillis(120))
                .map(event -> ServerSentEvent.<String>builder()
                        .id(event.eventId())
                        .event(event.type())
                        .data(event.data())
                        .build());
    }
}
