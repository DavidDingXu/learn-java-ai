package com.xiaoding.javaai.labs.agentscope;

public record A2aTaskRequest(
        String idempotencyKey,
        String remoteAgent,
        String instruction,
        String requestHash) {

    public A2aTaskRequest {
        if (idempotencyKey == null || idempotencyKey.isBlank() || remoteAgent == null || remoteAgent.isBlank()
                || instruction == null || instruction.isBlank() || requestHash == null || requestHash.isBlank()) {
            throw new IllegalArgumentException("A2A request fields must not be blank");
        }
    }
}
