package cn.dingxu.javaai.streaming;

import cn.dingxu.javaai.streaming.service.StreamEvent;
import cn.dingxu.javaai.streaming.service.StreamMetrics;
import cn.dingxu.javaai.streaming.service.StreamSessionService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StreamSessionServiceTest {

    @Test
    void createsOrderedTokenEventsAndDoneEvent() {
        StreamSessionService service = new StreamSessionService();

        List<StreamEvent> events = service.createEvents("s1001", "先核对订单，再检索退款制度。");

        assertThat(events).hasSize(3);
        assertThat(events.get(0).eventId()).isEqualTo("s1001-1");
        assertThat(events.get(0).type()).isEqualTo("token");
        assertThat(events.get(1).eventId()).isEqualTo("s1001-2");
        assertThat(events.get(2).eventId()).isEqualTo("s1001-3");
        assertThat(events.get(2).type()).isEqualTo("done");
    }

    @Test
    void resumesAfterLastEventId() {
        StreamSessionService service = new StreamSessionService();
        List<StreamEvent> events = service.createEvents("s1001", "先核对订单，再检索退款制度。");

        List<StreamEvent> resumed = service.resumeAfter(events, "s1001-1");

        assertThat(resumed)
                .extracting(StreamEvent::eventId)
                .containsExactly("s1001-2", "s1001-3");
    }

    @Test
    void createsHeartbeatEventWithoutMixingItWithTokenContent() {
        StreamSessionService service = new StreamSessionService();

        StreamEvent heartbeat = service.heartbeat("s1001", 1);

        assertThat(heartbeat.eventId()).isEqualTo("s1001-heartbeat-1");
        assertThat(heartbeat.type()).isEqualTo("heartbeat");
        assertThat(heartbeat.data()).isEqualTo("ping");
    }

    @Test
    void recordsTimeToFirstTokenFromRequestStart() {
        StreamSessionService service = new StreamSessionService();
        Instant requestStartedAt = Instant.parse("2026-06-07T04:00:00Z");
        Instant firstTokenAt = Instant.parse("2026-06-07T04:00:01.250Z");
        Instant completedAt = Instant.parse("2026-06-07T04:00:03Z");

        StreamMetrics metrics = service.metrics("s1001", requestStartedAt, firstTokenAt, completedAt);

        assertThat(metrics.sessionId()).isEqualTo("s1001");
        assertThat(metrics.ttftMs()).isEqualTo(1250);
        assertThat(metrics.totalLatencyMs()).isEqualTo(3000);
    }
}
