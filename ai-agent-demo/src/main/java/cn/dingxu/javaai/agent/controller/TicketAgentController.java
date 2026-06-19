package cn.dingxu.javaai.agent.controller;

import cn.dingxu.javaai.agent.service.TicketAgent;
import cn.dingxu.javaai.agent.service.TicketAgentRequest;
import cn.dingxu.javaai.agent.service.TicketAgentResult;
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
