package cn.dingxu.javaai.legacy.agent;

import cn.dingxu.javaai.legacy.agent.model.AgentTaskRequest;
import cn.dingxu.javaai.legacy.agent.model.AgentTaskResult;
import cn.dingxu.javaai.legacy.legacy.LegacyToolApiFacade;
import cn.dingxu.javaai.legacy.legacy.model.TicketSnapshot;

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
