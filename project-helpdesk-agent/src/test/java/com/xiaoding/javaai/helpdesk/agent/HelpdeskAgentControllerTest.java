package com.xiaoding.javaai.helpdesk.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HelpdeskAgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesRefundScenarioReport() throws Exception {
        mockMvc.perform(get("/api/helpdesk-agent/scenarios/refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value("T-1001"))
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.requiredAction").value("MANUAL_REVIEW"))
                .andExpect(jsonPath("$.citationDocumentIds", hasItem("refund-policy-2026")))
                .andExpect(jsonPath("$.toolNames", hasItem("ticket.lookup")))
                .andExpect(jsonPath("$.traceStepNames", hasItem("approval.plan")));
    }

    @Test
    void exposesAdviceEndpointWithOperatorContext() throws Exception {
        String body = """
                {
                  "ticketId": "T-1001",
                  "question": "客户申请退款，但订单已经发货，应该怎么处理？",
                  "userId": "u1001",
                  "tenantId": "tenant-a",
                  "department": "support"
                }
                """;

        mockMvc.perform(post("/api/helpdesk-agent/advice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advice", containsString("核对物流状态")))
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.requiredAction").value("MANUAL_REVIEW"))
                .andExpect(jsonPath("$.citations[0].documentId").value("refund-policy-2026"))
                .andExpect(jsonPath("$.trace.steps[0].name").value("ticket.lookup"));
    }

    @Test
    void exposesCloseTicketEndpointThatRequiresHumanApproval() throws Exception {
        String body = """
                {
                  "ticketId": "T-1001",
                  "humanApproved": false,
                  "userId": "u1001",
                  "tenantId": "tenant-a",
                  "department": "support"
                }
                """;

        mockMvc.perform(post("/api/helpdesk-agent/tickets/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("需要人工确认")));
    }

    @Test
    void exposesCloseTicketEndpointWithConfirmationToken() throws Exception {
        String body = """
                {
                  "ticketId": "T-1001",
                  "humanApproved": true,
                  "confirmationToken": "confirm-close-1001",
                  "userId": "lead-1",
                  "tenantId": "tenant-a",
                  "department": "support"
                }
                """;

        mockMvc.perform(post("/api/helpdesk-agent/tickets/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }
}
