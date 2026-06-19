package com.xiaoding.javaai.agent.controller;

import com.xiaoding.javaai.agent.service.TicketAgent;
import com.xiaoding.javaai.agent.service.TicketAgentRequest;
import com.xiaoding.javaai.agent.service.TicketAgentResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/tickets")
public class TicketAgentController {

    private final TicketAgent ticketAgent;

    public TicketAgentController(TicketAgent ticketAgent) {
        this.ticketAgent = ticketAgent;
    }

    @PostMapping("/advice")
    public TicketAgentResult advice(@RequestBody TicketAgentRequest request) {
        return ticketAgent.handle(request);
    }
}
