package com.xiaoding.javaai.agent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AiAgentApplicationContextTest {

    @Test
    void contextLoads() {
        assertThat(true).isTrue();
    }
}
