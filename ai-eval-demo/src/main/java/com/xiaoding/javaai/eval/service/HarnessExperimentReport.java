package com.xiaoding.javaai.eval.service;

import java.util.List;

public record HarnessExperimentReport(
        int total,
        int promotableCandidates,
        List<HarnessExperimentResult> results
) {
    public HarnessExperimentReport {
        results = results == null ? List.of() : List.copyOf(results);
        total = results.size();
        promotableCandidates = (int) results.stream().filter(HarnessExperimentResult::promotable).count();
    }
}
