package com.xiaoding.javaai.eval.agent;

public record AgentEvaluationTokens(String createToken, String runToken, String readToken) {
    public AgentEvaluationTokens {
        createToken = requireText(createToken, "createToken");
        runToken = requireText(runToken, "runToken");
        readToken = requireText(readToken, "readToken");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}
