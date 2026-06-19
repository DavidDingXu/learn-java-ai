package cn.dingxu.javaai.streaming.service;

public record StreamEvent(
        String eventId,
        String type,
        String data
) {
}
