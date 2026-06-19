package com.xiaoding.javaai.mcp;

import java.util.Map;

public record McpToolResult(boolean success, String message, Map<String, Object> data) {
    public McpToolResult {
        message = message == null ? "" : message;
        data = data == null ? Map.of() : Map.copyOf(data);
    }

    public static McpToolResult ok(String message, Map<String, Object> data) {
        return new McpToolResult(true, message, data);
    }

    public static McpToolResult failed(String message, Map<String, Object> data) {
        return new McpToolResult(false, message, data);
    }
}
