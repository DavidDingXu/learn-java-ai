package com.xiaoding.javaai.mcp;

import java.util.List;

public record McpServerManifest(
        String serverName,
        List<McpToolDescriptor> tools,
        List<McpResourceDescriptor> resources,
        List<McpPromptDescriptor> prompts
) {
    public McpServerManifest {
        if (serverName == null || serverName.isBlank()) {
            throw new IllegalArgumentException("serverName must not be blank");
        }
        tools = tools == null ? List.of() : List.copyOf(tools);
        resources = resources == null ? List.of() : List.copyOf(resources);
        prompts = prompts == null ? List.of() : List.copyOf(prompts);
    }
}
