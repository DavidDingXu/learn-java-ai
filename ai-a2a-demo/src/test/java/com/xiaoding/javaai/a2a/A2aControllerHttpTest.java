package com.xiaoding.javaai.a2a;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class A2aControllerHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void servesFrontendDemoPage() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("A2A Agent Skill")))
                .andExpect(content().string(containsString("/api/a2a/card")))
                .andExpect(content().string(containsString("/api/a2a/tasks")));
    }

    @Test
    void exposesAgentCardTaskAndResumeEndpoints() throws Exception {
        mockMvc.perform(get("/api/a2a/card"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("helpdesk-agent"))
                .andExpect(jsonPath("$.skills[*].id", hasItem("ticket.advice")));

        String response = mockMvc.perform(post("/api/a2a/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "skillId": "ticket.advice",
                                  "input": {
                                    "ticketId": "T-1001",
                                    "question": "客户申请退款但订单已发货怎么办"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("COMPLETED"))
                .andExpect(jsonPath("$.artifacts[0].name").value("ticket-advice"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String taskId = taskId(response);
        mockMvc.perform(get("/api/a2a/tasks/{taskId}/events", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].state", hasItem("COMPLETED")));

        String inputRequired = mockMvc.perform(post("/api/a2a/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "skillId": "ticket.advice",
                                  "input": {
                                    "ticketId": "T-1002",
                                    "question": "高金额退款是否可以直接处理",
                                    "approvalRequired": true
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("INPUT_REQUIRED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String inputRequiredTaskId = taskId(inputRequired);
        mockMvc.perform(post("/api/a2a/tasks/{taskId}/input", inputRequiredTaskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approved": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("COMPLETED"))
                .andExpect(jsonPath("$.statusMessage").value("ticket advice completed after human approval"));
    }

    private String taskId(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        return root.path("taskId").asText();
    }
}
