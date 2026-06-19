package com.xiaoding.javaai.mcp;

import java.util.List;

public record McpPromptDescriptor(String name, String description, List<String> requiredArguments) {
    public McpPromptDescriptor {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("prompt name must not be blank");
        }
        description = description == null ? "" : description;
        requiredArguments = requiredArguments == null ? List.of() : List.copyOf(requiredArguments);
    }
}
