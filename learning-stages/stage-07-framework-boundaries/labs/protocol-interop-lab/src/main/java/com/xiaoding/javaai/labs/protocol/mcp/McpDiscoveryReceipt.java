package com.xiaoding.javaai.labs.protocol.mcp;

import java.util.List;

public record McpDiscoveryReceipt(
        String protocolVersion,
        String serverName,
        String serverVersion,
        List<String> registeredTools
) {
    public McpDiscoveryReceipt {
        registeredTools = List.copyOf(registeredTools);
    }
}
