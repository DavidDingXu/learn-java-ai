package com.xiaoding.javaai.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TicketAgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesReadmeAdviceEndpointThroughHttpBinding() throws Exception {
        String body = """
                {
                  "ticketId": "T-1001",
                  "userQuestion": "客户申请 5000 元退款，但订单已经发货，能直接关闭工单吗？",
                  "userId": "u1001",
                  "tenantId": "tenant-a",
                  "department": "support"
                }
                """;

        mockMvc.perform(post("/api/agent/tickets/advice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advice", containsString("核对物流状态")))
                .andExpect(jsonPath("$.requiresHumanApproval").value(true))
                .andExpect(jsonPath("$.steps[*].name", hasItem("查询当前工单")))
                .andExpect(jsonPath("$.steps[*].name", hasItem("生成处理建议")));
    }

    @Test
    void exposesContextLabEndpointThroughHttpBinding() throws Exception {
        String body = """
                {
                  "tenantId": "tenant-a",
                  "sessionId": "s-lab",
                  "businessId": "T-1001",
                  "userId": "u1001",
                  "maxEstimatedTokens": 140,
                  "maxConversationMessages": 3
                }
                """;

        int firstTokenCount = readContextTokenCount(body);
        int secondTokenCount = readContextTokenCount(body);

        org.assertj.core.api.Assertions.assertThat(secondTokenCount).isEqualTo(firstTokenCount);
    }

    private int readContextTokenCount(String body) throws Exception {
        String response = mockMvc.perform(post("/api/agent/lab/context")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEstimatedTokens", lessThanOrEqualTo(140)))
                .andExpect(jsonPath("$.sources", hasItem("conversation")))
                .andExpect(jsonPath("$.sources", hasItem("businessSnapshot")))
                .andExpect(jsonPath("$.slices[*].type", hasItem("BUSINESS_SNAPSHOT")))
                .andExpect(jsonPath("$.slices[*].type", hasItem("CONVERSATION")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.totalEstimatedTokens");
    }

    @Test
    void exposesReactHookLabEndpointThroughHttpBinding() throws Exception {
        mockMvc.perform(post("/api/agent/lab/react")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "pii",
                                  "userInput": "请处理退款，手机号 13812345678",
                                  "maxSteps": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.steps[*].hookEvents", hasItem(hasItem("pii-masking:MASK_PHONE"))))
                .andExpect(jsonPath("$.steps[0].actionInput", containsString("138****5678")));

        mockMvc.perform(post("/api/agent/lab/react")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scenario": "blockedTool",
                                  "userInput": "直接退款",
                                  "maxSteps": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.stopReason").value("hook_rejected:tool-allowlist"))
                .andExpect(jsonPath("$.steps[0].hookEvents", hasItem("tool-allowlist:REJECT_TOOL")));
    }
}
