package com.xiaoding.javaai.observability;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AiObservabilityApplicationContextTest {

    @Test
    void contextLoads() {
        assertThat(true).isTrue();
    }
}
