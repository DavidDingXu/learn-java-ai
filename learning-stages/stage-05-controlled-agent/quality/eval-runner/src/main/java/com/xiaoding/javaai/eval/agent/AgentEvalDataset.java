package com.xiaoding.javaai.eval.agent;

import java.util.List;

public record AgentEvalDataset(String version, List<AgentEvalCase> cases) {
    public AgentEvalDataset {
        if (version == null || version.isBlank()) throw new IllegalArgumentException("version must not be blank");
        version = version.trim();
        cases = List.copyOf(cases);
        if (cases.isEmpty()) throw new IllegalArgumentException("agent eval cases must not be empty");
    }
}
