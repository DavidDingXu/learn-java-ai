package com.xiaoding.javaai.observability;

import com.xiaoding.javaai.observability.service.QuotaDecision;
import com.xiaoding.javaai.observability.service.QuotaPolicy;
import com.xiaoding.javaai.observability.service.QuotaService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuotaServiceTest {

    @Test
    void allowsRequestWhenUserAndTenantHaveEnoughQuota() {
        QuotaService quotaService = new QuotaService(new QuotaPolicy(1_000, 5_000));

        QuotaDecision decision = quotaService.checkAndConsume("tenant-a", "u1001", 300);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.reason()).isEqualTo("allowed");
        assertThat(decision.userRemaining()).isEqualTo(700);
        assertThat(decision.tenantRemaining()).isEqualTo(4_700);
    }

    @Test
    void rejectsWhenUserQuotaWouldBeExceeded() {
        QuotaService quotaService = new QuotaService(new QuotaPolicy(1_000, 5_000));

        quotaService.checkAndConsume("tenant-a", "u1001", 900);
        QuotaDecision decision = quotaService.checkAndConsume("tenant-a", "u1001", 200);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo("user_quota_exceeded");
        assertThat(decision.userRemaining()).isEqualTo(100);
    }

    @Test
    void rejectsWhenTenantQuotaWouldBeExceeded() {
        QuotaService quotaService = new QuotaService(new QuotaPolicy(1_000, 1_000));

        quotaService.checkAndConsume("tenant-a", "u1001", 700);
        QuotaDecision decision = quotaService.checkAndConsume("tenant-a", "u1002", 400);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo("tenant_quota_exceeded");
        assertThat(decision.tenantRemaining()).isEqualTo(300);
    }
}
