package com.xiaoding.javaai.output;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StructuredOutputControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesReadmeParseEndpointThroughHttpBinding() throws Exception {
        String rawJson = """
                {
                  "summary": "客户申请退款但订单已经发货",
                  "riskLevel": "MEDIUM",
                  "nextActions": ["核对物流状态", "转人工复核"],
                  "citations": ["refund-policy-001"]
                }
                """;

        mockMvc.perform(post("/api/output/ticket-advice/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary", containsString("订单已经发货")))
                .andExpect(jsonPath("$.riskLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.nextActions[0]").value("核对物流状态"))
                .andExpect(jsonPath("$.citations[0]").value("refund-policy-001"));
    }
}
