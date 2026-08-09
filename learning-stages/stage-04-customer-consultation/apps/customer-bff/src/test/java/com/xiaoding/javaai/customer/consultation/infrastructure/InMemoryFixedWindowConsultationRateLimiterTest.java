package com.xiaoding.javaai.customer.consultation.infrastructure;

import com.xiaoding.javaai.customer.identity.CustomerIdentity;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryFixedWindowConsultationRateLimiterTest {

    @Test
    void limits_per_tenant_and_customer_and_resets_after_the_window() {
        InMemoryFixedWindowConsultationRateLimiter limiter =
                new InMemoryFixedWindowConsultationRateLimiter(2, Duration.ofMinutes(1));
        CustomerIdentity customer = new CustomerIdentity(
                "customer-42", "tenant-a", List.of("customer"), List.of());
        Instant now = Instant.parse("2026-07-13T04:00:00Z");

        assertThat(limiter.tryAcquire(customer, now)).isTrue();
        assertThat(limiter.tryAcquire(customer, now.plusSeconds(1))).isTrue();
        assertThat(limiter.tryAcquire(customer, now.plusSeconds(2))).isFalse();
        assertThat(limiter.tryAcquire(customer, now.plusSeconds(61))).isTrue();
    }
}
