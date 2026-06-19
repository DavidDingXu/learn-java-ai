package com.xiaoding.javaai.mcp;

public record McpResourceDescriptor(String uri, String name, String mimeType) {
    public McpResourceDescriptor {
        if (uri == null || uri.isBlank()) {
            throw new IllegalArgumentException("resource uri must not be blank");
        }
        name = name == null ? uri : name;
        mimeType = mimeType == null ? "text/plain" : mimeType;
    }
}
