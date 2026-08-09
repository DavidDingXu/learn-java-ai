package com.xiaoding.javaai.customer.identity;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDelegatedTokenClientTest {

    @Test
    void creates_a_short_lived_local_token_without_an_identity_provider() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-02T01:00:00Z"), ZoneOffset.UTC);
        LocalDelegatedTokenClient client = new LocalDelegatedTokenClient(clock, Duration.ofMinutes(5));
        CustomerAccessToken source = new CustomerAccessToken(
                "local-customer-access",
                new CustomerIdentity("local-user", "tenant-a", List.of(), List.of()));

        DelegatedAccessToken token = client.exchange(source).block();

        assertThat(token).isNotNull();
        assertThat(token.tokenValue()).isEqualTo("local-delegated-access");
        assertThat(token.expiresAt()).isEqualTo(Instant.parse("2026-08-02T01:05:00Z"));
    }
}
