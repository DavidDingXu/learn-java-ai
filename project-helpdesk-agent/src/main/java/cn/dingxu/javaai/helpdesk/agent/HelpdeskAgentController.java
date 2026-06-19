package cn.dingxu.javaai.helpdesk.agent;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/helpdesk-agent")
public class HelpdeskAgentController {

    private final HelpdeskAgentApplicationService agentService;

    public HelpdeskAgentController(HelpdeskAgentApplicationService agentService) {
        this.agentService = agentService;
    }

    @GetMapping("/scenarios/refund")
    public HelpdeskAgentScenarioReport refundScenario() {
        return agentService.runRefundScenario();
    }

    @PostMapping("/advice")
    public AgentAdviceResult advice(@RequestBody AdviceHttpRequest request) {
        return agentService.advise(new AgentAdviceRequest(
                request.ticketId(),
                request.question(),
                request.operator()
        ));
    }

    @PostMapping("/tickets/close")
    public ToolResult closeTicket(@RequestBody CloseTicketHttpRequest request) {
        return agentService.closeTicket(
                request.ticketId(),
                request.humanApproved(),
                request.confirmationToken(),
                request.operator()
        );
    }

    public record AdviceHttpRequest(
            String ticketId,
            String question,
            String userId,
            String tenantId,
            String department
    ) {
        OperatorContext operator() {
            return new OperatorContext(userId, tenantId, department);
        }
    }

    public record CloseTicketHttpRequest(
            String ticketId,
            boolean humanApproved,
            String confirmationToken,
            String userId,
            String tenantId,
            String department
    ) {
        OperatorContext operator() {
            return new OperatorContext(userId, tenantId, department);
        }
    }
}
