package com.xiaoding.javaai.labs.protocol.a2a;

import java.util.List;

public record A2aDiscoveryReceipt(
        String agentName,
        String agentVersion,
        String protocolVersion,
        List<String> approvedSkills
) {
    public A2aDiscoveryReceipt {
        approvedSkills = List.copyOf(approvedSkills);
    }
}
