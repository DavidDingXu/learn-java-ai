package com.xiaoding.javaai.eval.agent;

public record AgentEvaluationTokens(String createToken, String runToken, String readToken) {
    public AgentEvaluationTokens {
        createToken = normalize(createToken);
        runToken = normalize(runToken);
        readToken = normalize(readToken);
        int configured = (createToken == null ? 0 : 1)
                + (runToken == null ? 0 : 1)
                + (readToken == null ? 0 : 1);
        if (configured != 0 && configured != 3) {
            throw new IllegalArgumentException("configure all three agent tokens or none of them");
        }
    }

    public static AgentEvaluationTokens none() {
        return new AgentEvaluationTokens(null, null, null);
    }

    public boolean configured() {
        return createToken != null;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
