package com.xiaoding.javaai.eval;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AiEvalApplicationContextTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
        assertThat(true).isTrue();
    }

    @Test
    void servesFrontendDemoPage() {
        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("Eval")
                .contains("/api/eval/run")
                .contains("/api/eval/judge/calibrate")
                .contains("/api/eval/prompt/regression")
                .contains("/api/eval/harness/run")
                .contains("policy.search")
                .contains("通过率")
                .contains("Judge 校准");
    }
}
