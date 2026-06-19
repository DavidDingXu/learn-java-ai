package com.xiaoding.javaai.streaming.service;

public record StreamEvent(
        String eventId,
        String type,
        String data
) {
}
