package com.xiaoding.javaai.ticket.agent.infrastructure;

import com.xiaoding.javaai.ticket.agent.application.AgentPlanningContext;
import com.xiaoding.javaai.ticket.agent.application.AgentPlanningResult;
import com.xiaoding.javaai.ticket.agent.application.TicketAgentPlanner;

final class DisabledTicketAgentPlanner implements TicketAgentPlanner {
    @Override
    public AgentPlanningResult plan(AgentPlanningContext context) {
        throw new AgentModelNotConfiguredException();
    }
}
