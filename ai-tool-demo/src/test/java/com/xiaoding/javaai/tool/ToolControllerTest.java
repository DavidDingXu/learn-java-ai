package com.xiaoding.javaai.tool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ToolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesLookupCloseAndLedgerEndpointsThroughHttpBinding() throws Exception {
        String operator = """
                {
                  "userId": "u1001",
                  "tenantId": "tenant-a",
                  "department": "support"
                }
                """;

        mockMvc.perform(post("/api/tools/ticket/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticketId": "T-1001",
                                  "operator": %s
                                }
                                """.formatted(operator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        mockMvc.perform(post("/api/tools/ticket/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticketId": "T-1001",
                                  "humanApproved": false,
                                  "operator": %s
                                }
                                """.formatted(operator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("人工确认")));

        mockMvc.perform(post("/api/tools/ticket/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ticketId": "T-1001",
                                  "humanApproved": true,
                                  "confirmationToken": "confirm-close-1001",
                                  "operator": %s
                                }
                                """.formatted(operator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        mockMvc.perform(get("/api/tools/ledger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$[0].toolName").value("ticket.lookup"));
    }
}
