package cn.dingxu.javaai.observability.service;

import java.util.ArrayList;
import java.util.List;

public class ReleaseReadinessChecker {

    private static final int P95_LATENCY_LIMIT_MS = 2_500;
    private static final int P95_LATENCY_WARNING_MS = 2_000;
    private static final double TOOL_FAILURE_RATE_LIMIT = 0.05;
    private static final double TOOL_FAILURE_RATE_WARNING = 0.04;
    private static final double BAD_CASE_RATE_LIMIT = 0.05;
    private static final double BAD_CASE_RATE_WARNING = 0.04;

    public ReleaseReadinessReport check(ReleaseCheckEvidence evidence) {
        List<String> failedChecks = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (!evidence.loadTestPassed()) {
            failedChecks.add("load_test");
        }
        if (!evidence.securityReviewPassed()) {
            failedChecks.add("security_review");
        }
        if (!evidence.grayReleasePlanReady()) {
            failedChecks.add("gray_release");
        }
        if (!evidence.rollbackPlanReady()) {
            failedChecks.add("rollback");
        }
        if (!evidence.onCallReady()) {
            failedChecks.add("on_call");
        }

        checkMetric("p95_latency", evidence.maxP95LatencyMs(), P95_LATENCY_WARNING_MS, P95_LATENCY_LIMIT_MS,
                failedChecks, warnings);
        checkMetric("tool_failure_rate", evidence.maxToolFailureRate(), TOOL_FAILURE_RATE_WARNING, TOOL_FAILURE_RATE_LIMIT,
                failedChecks, warnings);
        checkMetric("bad_case_rate", evidence.maxBadCaseRate(), BAD_CASE_RATE_WARNING, BAD_CASE_RATE_LIMIT,
                failedChecks, warnings);

        return new ReleaseReadinessReport(failedChecks.isEmpty(), failedChecks, warnings);
    }

    private void checkMetric(String name,
                             double value,
                             double warningThreshold,
                             double failureThreshold,
                             List<String> failedChecks,
                             List<String> warnings) {
        if (value <= 0) {
            return;
        }
        if (value > failureThreshold) {
            failedChecks.add(name);
            return;
        }
        if (value >= warningThreshold) {
            warnings.add(name + "_close_to_limit");
        }
    }
}
