package cn.dingxu.javaai.legacy.legacy;

import cn.dingxu.javaai.legacy.agent.LegacyAgentClient;
import cn.dingxu.javaai.legacy.agent.model.AgentTaskRequest;
import cn.dingxu.javaai.legacy.agent.model.AgentTaskResult;
import cn.dingxu.javaai.legacy.legacy.model.OperatorContext;
import cn.dingxu.javaai.legacy.legacy.model.TicketRecord;

public class LegacyTicketSystem {

    private final InMemoryTicketRepository ticketRepository;
    private final InMemoryLegacyAuditLedger auditLedger;
    private final LegacyAgentClient agentClient;

    public LegacyTicketSystem(InMemoryTicketRepository ticketRepository,
                              InMemoryLegacyAuditLedger auditLedger,
                              LegacyAgentClient agentClient) {
        this.ticketRepository = ticketRepository;
        this.auditLedger = auditLedger;
        this.agentClient = agentClient;
    }

    public AgentTaskResult requestAiAdvice(String ticketId, String question, OperatorContext operatorContext) {
        if (!operatorContext.hasPermission("AI_TICKET_ADVICE")) {
            throw new IllegalArgumentException("operator missing permission: AI_TICKET_ADVICE");
        }
        TicketRecord ticketRecord = ticketRepository.getRequired(ticketId);
        new LegacyToolApiFacade(ticketRepository).verifyCanRead(ticketRecord, operatorContext);
        auditLedger.record("SUBMIT_AGENT_TASK", ticketId, operatorContext);
        return agentClient.requestAdvice(new AgentTaskRequest("task-" + ticketId, ticketId, question, operatorContext));
    }
}
