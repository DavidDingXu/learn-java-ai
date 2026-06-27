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

@SpringBootTest(properties = {
        "spring.ai.openai.api-key=demo-key",
        "java-ai.output.model-name=gpt-4o-mini"
})
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

    @Test
    void returnsBadRequestWhenModelJsonCannotBecomeBusinessObject() throws Exception {
        String rawJson = """
                {
                  "summary": "客户申请退款但订单已经发货",
                  "riskLevel": "MEDIUM",
                  "nextActions": [],
                  "citations": ["refund-policy-001"]
                }
                """;

        mockMvc.perform(post("/api/output/ticket-advice/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rawJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STRUCTURED_OUTPUT_INVALID"))
                .andExpect(jsonPath("$.message", containsString("cannot become a valid business object")));
    }

    @Test
    void returnsBadRequestWhenModelOutputIsNotJson() throws Exception {
        mockMvc.perform(post("/api/output/ticket-advice/parse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("客户申请退款，但这不是 JSON"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STRUCTURED_OUTPUT_INVALID"))
                .andExpect(jsonPath("$.detail", containsString("not valid JSON")));
    }

    @Test
    void generatesTicketAdviceWithSpringAiPromptContract() throws Exception {
        String request = """
                {
                  "ticket": "客户申请退款但订单已经发货",
                  "policy": "已发货订单需要先核对物流状态，再决定是否转人工复核"
                }
                """;

        mockMvc.perform(post("/api/output/ticket-advice/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("sample"))
                .andExpect(jsonPath("$.prompt", containsString("Your response should be in JSON format")))
                .andExpect(jsonPath("$.prompt", containsString("riskLevel")))
                .andExpect(jsonPath("$.prompt", containsString("nextActions")))
                .andExpect(jsonPath("$.prompt", containsString("additionalProperties")))
                .andExpect(jsonPath("$.rawOutput").doesNotExist())
                .andExpect(jsonPath("$.advice.summary", containsString("订单已经发货")))
                .andExpect(jsonPath("$.advice.riskLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.advice.nextActions[0]").value("核对物流状态"))
                .andExpect(jsonPath("$.advice.citations[0]").value("refund-policy-001"));
    }
}
