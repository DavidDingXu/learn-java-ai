package com.xiaoding.javaai.observability;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiTraceControllerHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesTraceLifecycleEndpointsThroughHttpBinding() throws Exception {
        MvcResult started = mockMvc.perform(post("/api/traces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "u1001",
                                  "scenario": "ticket-advice"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andReturn();
        String traceId = JsonPath.read(started.getResponse().getContentAsString(), "$.traceId");

        mockMvc.perform(post("/api/traces/{traceId}/prompt", traceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateVersion": "ticket-advice-v1",
                                  "variables": ["ticketId", "policy"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("prompt.render"));

        mockMvc.perform(post("/api/traces/{traceId}/tool", traceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toolName": "ticket.lookup",
                                  "argsDigest": "ticketId=T-1001",
                                  "resultStatus": "OK"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attributes.toolName").value("ticket.lookup"));

        mockMvc.perform(post("/api/traces/{traceId}/events", traceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "stream.first-token",
                                  "attributes": {
                                    "ttftMs": 320
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("stream.first-token"));

        mockMvc.perform(post("/api/traces/{traceId}/model-usage", traceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "model": "gpt-4o-mini",
                                  "inputTokens": 120,
                                  "outputTokens": 80,
                                  "cost": 0.0012
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/traces/{traceId}", traceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trace.traceId").value(traceId))
                .andExpect(jsonPath("$.spans[*].name", hasItem("prompt.render")))
                .andExpect(jsonPath("$.events[0].name").value("stream.first-token"))
                .andExpect(jsonPath("$.cost").value(0.0012));
    }

    @Test
    void exposesQuotaFeedbackQualityAndCostEndpointsThroughHttpBinding() throws Exception {
        mockMvc.perform(post("/api/traces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": "u2001",
                                  "scenario": "ticket-advice"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/traces/quota/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": "tenant-a",
                                  "userId": "u1001",
                                  "requestedTokens": 500
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true));

        mockMvc.perform(post("/api/traces/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "traceId": "trace-frontend-demo",
                                  "scenario": "ticket-advice",
                                  "rating": "bad",
                                  "reason": "引用了错误制度"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason", containsString("错误制度")));

        mockMvc.perform(get("/api/traces/quality/{scenario}", "ticket-advice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.bad").value(1));

        mockMvc.perform(get("/api/traces/cost/by-scenario"))
                .andExpect(status().isOk());
    }
}
