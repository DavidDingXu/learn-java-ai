package com.xiaoding.javaai.legacy.agent;

import com.xiaoding.javaai.legacy.agent.model.AgentTaskRequest;
import com.xiaoding.javaai.legacy.agent.model.AgentTaskResult;
import com.xiaoding.javaai.legacy.legacy.LegacyToolApiFacade;
import com.xiaoding.javaai.legacy.legacy.model.TicketSnapshot;

import java.util.Collections;

public class ExternalAgentService {

    private final LegacyToolApiFacade legacyToolApiFacade;

    public ExternalAgentService(LegacyToolApiFacade legacyToolApiFacade) {
        this.legacyToolApiFacade = legacyToolApiFacade;
    }

    public AgentTaskResult handle(AgentTaskRequest request) {
        TicketSnapshot snapshot = legacyToolApiFacade.queryTicket(request.getTicketId(), request.getOperatorContext());
        boolean highRisk = request.getQuestion().contains("关闭") || snapshot.getContent().contains("退款");
        String advice = "订单已发货，Agent 只能通过 Tool API 读取工单快照。建议先核对物流，再按制度给出处理建议。";
        return AgentTaskResult.completed(
                request.getTaskId(),
                advice,
                highRisk,
                "trace-" + request.getTaskId(),
                Collections.singletonList(snapshot)
        );
    }
}
