package com.xiaoding.javaai.labs.alibaba;

import java.util.Map;
import java.time.Duration;

public record ProviderAnswer(
        String text,
        String model,
        ProviderUsage usage,
        Duration latency,
        Map<String, Object> providerMetadata) {
    public ProviderAnswer {
        if (usage == null) usage = ProviderUsage.unavailable();
        if (latency == null || latency.isNegative()) {
            throw new IllegalArgumentException("latency must not be null or negative");
        }
        providerMetadata = Map.copyOf(providerMetadata);
    }
}
