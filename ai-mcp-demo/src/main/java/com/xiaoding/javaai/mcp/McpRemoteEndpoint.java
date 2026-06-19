package com.xiaoding.javaai.mcp;

import java.time.Duration;

public record McpRemoteEndpoint(
        String url,
        McpTransportType transportType,
        PolicyMcpServer server,
        Duration connectTimeout
) {
    public McpRemoteEndpoint {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url must not be blank");
        }
        transportType = transportType == null ? McpTransportType.STREAMABLE_HTTP : transportType;
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
    }

    public boolean available() {
        return server != null;
    }
}
