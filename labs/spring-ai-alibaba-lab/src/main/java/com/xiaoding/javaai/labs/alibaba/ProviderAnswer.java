package com.xiaoding.javaai.labs.alibaba;

import java.util.Map;

public record ProviderAnswer(String text, String model, Map<String, Object> providerMetadata) {
    public ProviderAnswer {
        providerMetadata = Map.copyOf(providerMetadata);
    }
}
