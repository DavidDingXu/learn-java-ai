package cn.dingxu.javaai.observability.service;

import java.util.List;

public record ReleaseReadinessReport(
        boolean ready,
        List<String> failedChecks,
        List<String> warnings
) {
    public ReleaseReadinessReport {
        failedChecks = failedChecks == null ? List.of() : List.copyOf(failedChecks);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
