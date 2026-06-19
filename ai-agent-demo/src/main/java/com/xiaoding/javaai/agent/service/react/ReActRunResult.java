package com.xiaoding.javaai.agent.service.react;

import java.util.List;

public record ReActRunResult(boolean completed, String finalAnswer, String stopReason, List<ReActStep> steps) {

    public ReActRunResult {
        finalAnswer = finalAnswer == null ? "" : finalAnswer;
        stopReason = stopReason == null ? "" : stopReason;
        steps = steps == null ? List.of() : List.copyOf(steps);
    }

    public static ReActRunResult completed(String finalAnswer, List<ReActStep> steps) {
        return new ReActRunResult(true, finalAnswer, "finished", steps);
    }

    public static ReActRunResult stopped(String stopReason, List<ReActStep> steps) {
        return new ReActRunResult(false, "", stopReason, steps);
    }
}
