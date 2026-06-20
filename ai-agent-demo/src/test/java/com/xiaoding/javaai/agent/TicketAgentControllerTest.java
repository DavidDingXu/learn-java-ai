package com.xiaoding.javaai.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
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
}
