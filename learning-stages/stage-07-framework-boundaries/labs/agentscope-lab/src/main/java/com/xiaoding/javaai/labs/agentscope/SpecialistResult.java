package com.xiaoding.javaai.labs.agentscope;

public record SpecialistResult(String agentName, String answer, String error) {

    public SpecialistResult {
        if (agentName == null || agentName.isBlank()) {
            throw new IllegalArgumentException("agentName must not be blank");
        }
        boolean succeeded = answer != null && !answer.isBlank();
        boolean failed = error != null && !error.isBlank();
        if (succeeded == failed) {
            throw new IllegalArgumentException("specialist result must contain either answer or error");
        }
        agentName = agentName.strip();
        answer = succeeded ? answer.strip() : null;
        error = failed ? error.strip() : null;
    }

    public static SpecialistResult success(String agentName, String answer) {
        return new SpecialistResult(agentName, answer, null);
    }

    public static SpecialistResult failure(String agentName, String error) {
        return new SpecialistResult(agentName, null, error);
    }

    public boolean succeeded() {
        return error == null;
    }
}
