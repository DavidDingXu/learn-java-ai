package com.xiaoding.javaai.agent.service.react.hook;

import com.xiaoding.javaai.agent.service.react.ReActAction;

import java.util.List;

public record HookResult(
        ReActAction action,
        boolean rejected,
        String reason,
        List<String> events
) {
    public HookResult {
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        reason = reason == null ? "" : reason;
        events = events == null ? List.of() : List.copyOf(events);
    }

    public static HookResult accepted(ReActAction action, List<String> events) {
        return new HookResult(action, false, "", events);
    }

    public static HookResult rejected(ReActAction action, String reason, List<String> events) {
        return new HookResult(action, true, reason, events);
    }
}
