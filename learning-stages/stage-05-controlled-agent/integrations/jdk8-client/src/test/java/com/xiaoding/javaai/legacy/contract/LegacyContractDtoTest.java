package com.xiaoding.javaai.legacy.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LegacyContractDtoTest {

    @Test
    void contractDtoCompilesAndRunsOnJava8WithoutJava21ServiceTypes() throws Exception {
        ToolActionCommand command = new ToolActionCommand(
                "action-123",
                "ticket-456",
                "ADD_INTERNAL_NOTE",
                Collections.singletonMap("note", "已联系支付渠道")
        );

        assertEquals("ticket-456", command.getTicketId());
        assertEquals("ADD_INTERNAL_NOTE", command.getActionType());
        assertEquals("已联系支付渠道", command.getArguments().get("note"));
        assertFalse(new ObjectMapper().writeValueAsString(command).contains("idempotencyKey"));
    }
}
