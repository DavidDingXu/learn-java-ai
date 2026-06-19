package com.xiaoding.javaai.helpdesk.agent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HelpdeskAgentApplicationContextTest {

    @Test
    void contextLoads() {
        assertThat(true).isTrue();
    }
}
