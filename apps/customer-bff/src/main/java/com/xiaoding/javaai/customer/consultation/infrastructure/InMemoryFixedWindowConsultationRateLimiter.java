package com.xiaoding.javaai.customer.consultation.infrastructure;

import com.xiaoding.javaai.customer.consultation.application.port.ConsultationRateLimiter;
import com.xiaoding.javaai.customer.identity.CustomerIdentity;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class InMemoryFixedWindowConsultationRateLimiter implements ConsultationRateLimiter {

    private final int limit;
    private final Duration window;
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();

    public InMemoryFixedWindowConsultationRateLimiter(int limit, Duration window) {
        if (limit < 1) throw new IllegalArgumentException("limit must be positive");
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        this.limit = limit;
        this.window = window;
    }

    @Override
    public boolean tryAcquire(CustomerIdentity identity, Instant now) {
        String key = identity.tenantId() + ":" + identity.subject();
        AtomicBoolean accepted = new AtomicBoolean();
        counters.compute(key, (ignored, current) -> {
            if (current == null || !now.isBefore(current.windowStartedAt().plus(window))) {
                accepted.set(true);
                return new Counter(now, 1);
            }
            if (current.count() >= limit) {
                accepted.set(false);
                return current;
            }
            accepted.set(true);
            return new Counter(current.windowStartedAt(), current.count() + 1);
        });
        return accepted.get();
    }

    private record Counter(Instant windowStartedAt, int count) {
    }
}
