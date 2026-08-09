package com.xiaoding.javaai.customer.identity;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerJwtConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CustomerJwtConfiguration.class)
            .withPropertyValues(
                    "java-ai.security.mode=jwt",
                    "java-ai.security.jwt.issuer=https://identity.example.test",
                    "java-ai.security.jwt.audience=customer-bff",
                    "java-ai.security.jwt.hmac-secret=local-development-secret-must-have-at-least-32-bytes",
                    "java-ai.security.jwt.jwk-set-uri=https://identity.example.test/.well-known/jwks.json"
            );

    @Test
    void fails_startup_when_hmac_secret_and_jwk_set_uri_are_both_configured() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseMessage(
                            "Configure exactly one customer JWT verification source: jwk-set-uri or hmac-secret");
        });
    }
}
