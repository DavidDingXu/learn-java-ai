package com.xiaoding.javaai.mcp;

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
class McpControllerHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesFrontendDemoPage() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("MCP Demo")))
                .andExpect(content().string(containsString("/api/mcp/session")))
                .andExpect(content().string(containsString("/api/mcp/tools/call")));
    }

    @Test
    void exposesMcpSessionToolResourceAndDebugEndpoints() throws Exception {
        mockMvc.perform(get("/api/mcp/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serverName").value("policy-center-mcp"))
                .andExpect(jsonPath("$.tools", hasItem("policy.search")))
                .andExpect(jsonPath("$.prompts", hasItem("ticket-policy-answer")));

        mockMvc.perform(post("/api/mcp/tools/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toolName": "policy.search",
                                  "arguments": {"query": "退款"},
                                  "operator": {
                                    "userId": "u1001",
                                    "tenantId": "tenant-a",
                                    "department": "support",
                                    "permissions": []
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.matches[0].documentId").value("refund-policy-001"));

        mockMvc.perform(post("/api/mcp/tools/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toolName": "policy.get",
                                  "arguments": {"documentId": "refund-policy-001"},
                                  "operator": {
                                    "userId": "u1001",
                                    "tenantId": "tenant-a",
                                    "department": "support",
                                    "permissions": []
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("missing permission")));

        mockMvc.perform(post("/api/mcp/resources/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "uri": "policy://refund/refund-policy-001",
                                  "operator": {
                                    "userId": "u2001",
                                    "tenantId": "tenant-a",
                                    "department": "hr",
                                    "permissions": []
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visible").value(false));

        mockMvc.perform(get("/api/mcp/debug"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.tools", hasItem("policy.search")));
    }
}
