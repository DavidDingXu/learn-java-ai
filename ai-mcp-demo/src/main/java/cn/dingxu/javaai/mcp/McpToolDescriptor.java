package cn.dingxu.javaai.mcp;

import java.util.Map;

public record McpToolDescriptor(String name, String description, Map<String, String> inputSchema) {
    public McpToolDescriptor {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("tool name must not be blank");
        }
        description = description == null ? "" : description;
        inputSchema = inputSchema == null ? Map.of() : Map.copyOf(inputSchema);
    }
}
