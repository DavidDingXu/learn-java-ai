package cn.dingxu.javaai.helpdesk.agent;

import java.util.Map;

public record ToolResult(boolean success, String message, Map<String, Object> data) {
    public ToolResult {
        message = message == null ? "" : message;
        data = data == null ? Map.of() : Map.copyOf(data);
    }

    static ToolResult ok(String message, Map<String, Object> data) {
        return new ToolResult(true, message, data);
    }

    static ToolResult failed(String message, Map<String, Object> data) {
        return new ToolResult(false, message, data);
    }
}
