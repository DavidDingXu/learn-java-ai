package com.xiaoding.javaai.customer.consultation.application.port;

import com.xiaoding.javaai.customer.identity.CustomerIdentity;

import java.time.Instant;

@FunctionalInterface
public interface ConsultationRateLimiter {
    boolean tryAcquire(CustomerIdentity identity, Instant now);
}
