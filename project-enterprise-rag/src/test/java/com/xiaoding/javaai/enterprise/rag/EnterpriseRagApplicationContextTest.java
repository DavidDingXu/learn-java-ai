package com.xiaoding.javaai.enterprise.rag;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EnterpriseRagApplicationContextTest {

    @Test
    void contextLoads() {
        assertThat(true).isTrue();
    }
}
