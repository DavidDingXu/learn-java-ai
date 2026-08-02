package com.xiaoding.javaai.customer.identity;

import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;

public final class LocalDelegatedTokenClient implements DelegatedTokenClient {

    private static final String LOCAL_TOKEN = "local-delegated-access";

    private final Clock clock;
    private final Duration lifetime;

    public LocalDelegatedTokenClient(Clock clock, Duration lifetime) {
        this.clock = java.util.Objects.requireNonNull(clock, "clock must not be null");
        if (lifetime == null || lifetime.isZero() || lifetime.isNegative()) {
            throw new IllegalArgumentException("lifetime must be positive");
        }
        this.lifetime = lifetime;
    }

    @Override
    public Mono<DelegatedAccessToken> exchange(CustomerAccessToken source) {
        java.util.Objects.requireNonNull(source, "source must not be null");
        return Mono.fromSupplier(() -> new DelegatedAccessToken(
                LOCAL_TOKEN, clock.instant().plus(lifetime)));
    }
}
