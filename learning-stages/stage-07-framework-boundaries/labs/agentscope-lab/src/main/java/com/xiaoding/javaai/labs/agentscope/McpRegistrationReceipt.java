package com.xiaoding.javaai.labs.agentscope;

import java.net.URI;
import java.util.List;

public record McpRegistrationReceipt(String serverId, URI endpoint, List<String> registeredTools) {
    public McpRegistrationReceipt {
        registeredTools = List.copyOf(registeredTools);
    }
}
