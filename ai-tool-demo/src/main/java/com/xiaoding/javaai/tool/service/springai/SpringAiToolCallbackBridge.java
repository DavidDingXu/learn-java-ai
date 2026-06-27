package com.xiaoding.javaai.tool.service.springai;

import com.xiaoding.javaai.tool.service.OperatorContext;
import com.xiaoding.javaai.tool.service.TicketToolFacade;
import com.xiaoding.javaai.tool.service.ToolResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 * Exposes governed Tool API methods as Spring AI ToolCallbacks.
 */
public class SpringAiToolCallbackBridge {

    private final TicketToolFacade ticketToolFacade;

    public SpringAiToolCallbackBridge(TicketToolFacade ticketToolFacade) {
        this.ticketToolFacade = ticketToolFacade;
    }

    public ToolCallbackProvider toolCallbackProvider() {
        return ToolCallbackProvider.from(callbacks());
    }

    public ToolCallback[] callbacks() {
        return new ToolCallback[]{
                lookupTicketCallback(),
                closeTicketCallback()
        };
    }

    private ToolCallback lookupTicketCallback() {
        return FunctionToolCallback
                .builder("ticket.lookup", (TicketLookupToolInput input) ->
                        ticketToolFacade.lookupTicket(input.ticketId(), input.operator()))
                .description("查询工单最小上下文，结果会经过权限、参数校验和 Tool 审计。")
                .inputType(TicketLookupToolInput.class)
                .build();
    }

    private ToolCallback closeTicketCallback() {
        return FunctionToolCallback
                .builder("ticket.close", (TicketCloseToolInput input) ->
                        ticketToolFacade.closeTicket(
                                input.ticketId(),
                                input.humanApproved(),
                                input.confirmationToken(),
                                input.operator()))
                .description("关闭工单写操作，必须带人工确认和 confirmationToken。")
                .inputType(TicketCloseToolInput.class)
                .build();
    }

    public record TicketLookupToolInput(String ticketId, OperatorContext operator) {
    }

    public record TicketCloseToolInput(String ticketId,
                                       boolean humanApproved,
                                       String confirmationToken,
                                       OperatorContext operator) {
    }
}
