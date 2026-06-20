package com.xiaoding.javaai.gateway;

import com.xiaoding.javaai.gateway.client.ModelClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiGatewayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ModelClient modelClient;

    @BeforeEach
    void setUp() {
        given(modelClient.modelName()).willReturn("contract-test-model");
        given(modelClient.priority()).willReturn(0);
        given(modelClient.chat(contains("客户申请退款"))).willReturn("建议先核对订单发货状态，再按退款制度处理。");
    }

    @Test
    void exposesReadmeChatEndpointThroughHttpBinding() throws Exception {
        String body = """
                {
                  "userId": "u1001",
                  "message": "客户申请退款，但订单已经发货。"
                }
                """;

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.model").value("contract-test-model"))
                .andExpect(jsonPath("$.content", containsString("退款制度")))
                .andExpect(jsonPath("$.latencyMs").isNumber());
    }
}
