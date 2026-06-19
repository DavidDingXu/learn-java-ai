package cn.dingxu.javaai.observability;

import cn.dingxu.javaai.observability.service.ReleaseCheckEvidence;
import cn.dingxu.javaai.observability.service.ReleaseReadinessChecker;
import cn.dingxu.javaai.observability.service.ReleaseReadinessReport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleaseReadinessCheckerTest {

    @Test
    void blocksReleaseWhenLoadTestAndRollbackEvidenceAreMissing() {
        ReleaseCheckEvidence evidence = ReleaseCheckEvidence.builder()
                .securityReviewPassed(true)
                .grayReleasePlanReady(true)
                .onCallReady(true)
                .build();

        ReleaseReadinessReport report = new ReleaseReadinessChecker().check(evidence);

        assertFalse(report.ready());
        assertTrue(report.failedChecks().contains("load_test"));
        assertTrue(report.failedChecks().contains("rollback"));
    }

    @Test
    void allowsReleaseOnlyWhenAllProductionGatesPass() {
        ReleaseCheckEvidence evidence = ReleaseCheckEvidence.builder()
                .loadTestPassed(true)
                .securityReviewPassed(true)
                .grayReleasePlanReady(true)
                .rollbackPlanReady(true)
                .onCallReady(true)
                .maxP95LatencyMs(1800)
                .maxToolFailureRate(0.02)
                .maxBadCaseRate(0.03)
                .build();

        ReleaseReadinessReport report = new ReleaseReadinessChecker().check(evidence);

        assertTrue(report.ready());
        assertTrue(report.failedChecks().isEmpty());
        assertTrue(report.warnings().isEmpty());
    }

    @Test
    void warnsWhenMetricsAreCloseToReleaseThreshold() {
        ReleaseCheckEvidence evidence = ReleaseCheckEvidence.builder()
                .loadTestPassed(true)
                .securityReviewPassed(true)
                .grayReleasePlanReady(true)
                .rollbackPlanReady(true)
                .onCallReady(true)
                .maxP95LatencyMs(2400)
                .maxToolFailureRate(0.045)
                .maxBadCaseRate(0.045)
                .build();

        ReleaseReadinessReport report = new ReleaseReadinessChecker().check(evidence);

        assertTrue(report.ready());
        assertTrue(report.warnings().contains("p95_latency_close_to_limit"));
        assertTrue(report.warnings().contains("tool_failure_rate_close_to_limit"));
        assertTrue(report.warnings().contains("bad_case_rate_close_to_limit"));
    }
}
