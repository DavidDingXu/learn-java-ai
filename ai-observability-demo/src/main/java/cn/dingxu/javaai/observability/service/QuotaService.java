package cn.dingxu.javaai.observability.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class QuotaService {

    private final QuotaPolicy policy;
    private final Map<String, Integer> usedByUser = new LinkedHashMap<>();
    private final Map<String, Integer> usedByTenant = new LinkedHashMap<>();

    public QuotaService() {
        this(new QuotaPolicy(20_000, 200_000));
    }

    public QuotaService(QuotaPolicy policy) {
        this.policy = policy;
    }

    public synchronized QuotaDecision checkAndConsume(String tenantId, String userId, int requestedTokens) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (requestedTokens < 0) {
            throw new IllegalArgumentException("requestedTokens must not be negative");
        }

        String userKey = tenantId + ":" + userId;
        int userUsed = usedByUser.getOrDefault(userKey, 0);
        int tenantUsed = usedByTenant.getOrDefault(tenantId, 0);
        int userRemaining = policy.userTokenLimit() - userUsed;
        int tenantRemaining = policy.tenantTokenLimit() - tenantUsed;

        if (requestedTokens > userRemaining) {
            return new QuotaDecision(false, "user_quota_exceeded", userRemaining, tenantRemaining);
        }
        if (requestedTokens > tenantRemaining) {
            return new QuotaDecision(false, "tenant_quota_exceeded", userRemaining, tenantRemaining);
        }

        usedByUser.put(userKey, userUsed + requestedTokens);
        usedByTenant.put(tenantId, tenantUsed + requestedTokens);
        return new QuotaDecision(true, "allowed", userRemaining - requestedTokens, tenantRemaining - requestedTokens);
    }
}
