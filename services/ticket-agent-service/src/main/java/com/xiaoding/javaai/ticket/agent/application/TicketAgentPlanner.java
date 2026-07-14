package com.xiaoding.javaai.ticket.agent.application;

@FunctionalInterface
public interface TicketAgentPlanner {
    AgentPlanningResult plan(AgentPlanningContext context);
}
