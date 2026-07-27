package com.xiaoding.javaai.customer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class CustomerBffDemoStartupTest {

    @Autowired
    private Environment environment;

    @Test
    void defaultConfigurationStartsWithLocalDemoBoundaries() {
        assertThat(environment.acceptsProfiles(Profiles.of("demo"))).isTrue();
        assertThat(environment.getProperty(
                "java-ai.runtime.external-integrations-enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty(
                "java-ai.security.customer-jwt.enabled", Boolean.class)).isFalse();
    }
}
