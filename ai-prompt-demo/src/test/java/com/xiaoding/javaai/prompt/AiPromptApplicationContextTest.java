package com.xiaoding.javaai.prompt;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AiPromptApplicationContextTest {

    @Test
    void contextLoads() {
        assertThat(true).isTrue();
    }
}
