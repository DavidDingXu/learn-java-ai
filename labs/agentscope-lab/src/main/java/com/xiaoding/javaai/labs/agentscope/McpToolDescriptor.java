package com.xiaoding.javaai.labs.agentscope;

import java.util.Map;

public record McpToolDescriptor(
        String name,
        String description,
        boolean readOnly,
        Map<String, Object> inputSchema) {

    public McpToolDescriptor {
        inputSchema = Map.copyOf(inputSchema);
    }
}
