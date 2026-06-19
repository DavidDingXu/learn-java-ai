package com.xiaoding.javaai.streaming.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class StreamSessionService {

    public List<StreamEvent> createEvents(String sessionId, String content) {
        String[] tokens = splitContent(content);
        List<StreamEvent> events = new ArrayList<>();
        for (int i = 0; i < tokens.length; i++) {
            events.add(new StreamEvent(sessionId + "-" + (i + 1), "token", tokens[i]));
        }
        events.add(new StreamEvent(sessionId + "-" + (tokens.length + 1), "done", "[DONE]"));
        return events;
    }

    public List<StreamEvent> resumeAfter(List<StreamEvent> events, String lastEventId) {
        int start = 0;
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).eventId().equals(lastEventId)) {
                start = i + 1;
                break;
            }
        }
        return events.subList(start, events.size());
    }

    public StreamEvent heartbeat(String sessionId, int sequence) {
        return new StreamEvent(sessionId + "-heartbeat-" + sequence, "heartbeat", "ping");
    }

    public StreamMetrics metrics(String sessionId,
                                 Instant requestStartedAt,
                                 Instant firstTokenAt,
                                 Instant completedAt) {
        return new StreamMetrics(
                sessionId,
                requestStartedAt,
                firstTokenAt,
                completedAt,
                Duration.between(requestStartedAt, firstTokenAt).toMillis(),
                Duration.between(requestStartedAt, completedAt).toMillis()
        );
    }

    private String[] splitContent(String content) {
        return content.split("，|。");
    }
}
