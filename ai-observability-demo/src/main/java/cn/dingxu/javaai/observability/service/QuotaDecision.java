package cn.dingxu.javaai.observability.service;

public record QuotaDecision(
        boolean allowed,
        String reason,
        int userRemaining,
        int tenantRemaining
) {
    public QuotaDecision {
        reason = reason == null ? "" : reason;
    }
}
