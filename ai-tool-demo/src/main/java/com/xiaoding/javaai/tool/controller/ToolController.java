package com.xiaoding.javaai.tool.controller;

import com.xiaoding.javaai.tool.service.OperatorContext;
import com.xiaoding.javaai.tool.service.TicketToolFacade;
import com.xiaoding.javaai.tool.service.ToolExecutionLedger;
import com.xiaoding.javaai.tool.service.ToolExecutionRecord;
import com.xiaoding.javaai.tool.service.ToolResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tools")
public class ToolController {

    private final TicketToolFacade ticketToolFacade;
    private final ToolExecutionLedger ledger;

    public ToolController(TicketToolFacade ticketToolFacade, ToolExecutionLedger ledger) {
        this.ticketToolFacade = ticketToolFacade;
        this.ledger = ledger;
    }

    @PostMapping("/ticket/lookup")
    public ToolResult lookupTicket(@RequestBody TicketLookupRequest request) {
        return ticketToolFacade.lookupTicket(request.ticketId(), request.operator());
    }

    @PostMapping("/ticket/close")
    public ToolResult closeTicket(@RequestBody TicketCloseRequest request) {
        return ticketToolFacade.closeTicket(
                request.ticketId(),
                request.humanApproved(),
                request.confirmationToken(),
                request.operator()
        );
    }

    @GetMapping("/ledger")
    public List<ToolExecutionRecord> ledger() {
        return ledger.records();
    }

    public record TicketLookupRequest(String ticketId, OperatorContext operator) {
    }

    public record TicketCloseRequest(String ticketId,
                                     boolean humanApproved,
                                     String confirmationToken,
                                     OperatorContext operator) {
    }
}
