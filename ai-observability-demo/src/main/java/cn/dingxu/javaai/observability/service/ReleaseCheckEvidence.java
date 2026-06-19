package cn.dingxu.javaai.observability.service;

public record ReleaseCheckEvidence(
        boolean loadTestPassed,
        boolean securityReviewPassed,
        boolean grayReleasePlanReady,
        boolean rollbackPlanReady,
        boolean onCallReady,
        int maxP95LatencyMs,
        double maxToolFailureRate,
        double maxBadCaseRate
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean loadTestPassed;
        private boolean securityReviewPassed;
        private boolean grayReleasePlanReady;
        private boolean rollbackPlanReady;
        private boolean onCallReady;
        private int maxP95LatencyMs;
        private double maxToolFailureRate;
        private double maxBadCaseRate;

        public Builder loadTestPassed(boolean loadTestPassed) {
            this.loadTestPassed = loadTestPassed;
            return this;
        }

        public Builder securityReviewPassed(boolean securityReviewPassed) {
            this.securityReviewPassed = securityReviewPassed;
            return this;
        }

        public Builder grayReleasePlanReady(boolean grayReleasePlanReady) {
            this.grayReleasePlanReady = grayReleasePlanReady;
            return this;
        }

        public Builder rollbackPlanReady(boolean rollbackPlanReady) {
            this.rollbackPlanReady = rollbackPlanReady;
            return this;
        }

        public Builder onCallReady(boolean onCallReady) {
            this.onCallReady = onCallReady;
            return this;
        }

        public Builder maxP95LatencyMs(int maxP95LatencyMs) {
            this.maxP95LatencyMs = maxP95LatencyMs;
            return this;
        }

        public Builder maxToolFailureRate(double maxToolFailureRate) {
            this.maxToolFailureRate = maxToolFailureRate;
            return this;
        }

        public Builder maxBadCaseRate(double maxBadCaseRate) {
            this.maxBadCaseRate = maxBadCaseRate;
            return this;
        }

        public ReleaseCheckEvidence build() {
            return new ReleaseCheckEvidence(
                    loadTestPassed,
                    securityReviewPassed,
                    grayReleasePlanReady,
                    rollbackPlanReady,
                    onCallReady,
                    maxP95LatencyMs,
                    maxToolFailureRate,
                    maxBadCaseRate
            );
        }
    }
}
