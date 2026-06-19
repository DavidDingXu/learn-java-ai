package cn.dingxu.javaai.tool.service;

import java.util.Map;

public record ToolResult(boolean success, String message, Map<String, Object> data) {
    public ToolResult {
        message = message == null ? "" : message;
        data = data == null ? Map.of() : Map.copyOf(data);
    }

    public static ToolResult ok(String message, Map<String, Object> data) {
        return new ToolResult(true, message, data);
    }

    public static ToolResult failed(String message, Map<String, Object> data) {
        return new ToolResult(false, message, data);
    }
}
