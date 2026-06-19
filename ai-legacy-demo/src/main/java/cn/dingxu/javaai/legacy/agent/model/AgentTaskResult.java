package cn.dingxu.javaai.legacy.agent.model;

import cn.dingxu.javaai.legacy.legacy.model.TicketSnapshot;

import java.util.Collections;
import java.util.List;

public class AgentTaskResult {

    private final String taskId;
    private final String advice;
    private final boolean requiresHumanApproval;
    private final String traceId;
    private final List<TicketSnapshot> toolSnapshots;

    private AgentTaskResult(String taskId,
                            String advice,
                            boolean requiresHumanApproval,
                            String traceId,
                            List<TicketSnapshot> toolSnapshots) {
        this.taskId = taskId;
        this.advice = advice;
        this.requiresHumanApproval = requiresHumanApproval;
        this.traceId = traceId;
        this.toolSnapshots = Collections.unmodifiableList(toolSnapshots);
    }

    public static AgentTaskResult completed(String taskId,
                                            String advice,
                                            boolean requiresHumanApproval,
                                            String traceId,
                                            List<TicketSnapshot> toolSnapshots) {
        return new AgentTaskResult(taskId, advice, requiresHumanApproval, traceId, toolSnapshots);
    }

    public String getTaskId() {
        return taskId;
    }

    public String getAdvice() {
        return advice;
    }

    public boolean isRequiresHumanApproval() {
        return requiresHumanApproval;
    }

    public String getTraceId() {
        return traceId;
    }

    public List<TicketSnapshot> getToolSnapshots() {
        return toolSnapshots;
    }
}
