package com.xiaoding.javaai.customer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class CustomerBffDefaultStartupTest {

    @Autowired
    private Environment environment;

    @Test
    void default_configuration_uses_local_identity_and_real_downstream_http_clients() {
        assertThat(environment.getActiveProfiles()).isEmpty();
        assertThat(environment.getProperty("java-ai.security.mode")).isEqualTo("fixed");
        assertThat(environment.getProperty("java-ai.identity.delegation-mode")).isEqualTo("local");
        assertThat(environment.getProperty("java-ai.downstream.knowledge.base-url"))
                .isEqualTo("http://localhost:8081");
        assertThat(environment.getProperty("java-ai.downstream.ticket.base-url"))
                .isEqualTo("http://localhost:8082");
    }
}
