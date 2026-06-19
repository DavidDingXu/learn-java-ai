package cn.dingxu.javaai.legacy.agent.model;

import cn.dingxu.javaai.legacy.legacy.model.OperatorContext;

public class AgentTaskRequest {

    private static final String CONTRACT_NAME = "AgentTask API";

    private final String taskId;
    private final String ticketId;
    private final String question;
    private final OperatorContext operatorContext;

    public AgentTaskRequest(String taskId, String ticketId, String question, OperatorContext operatorContext) {
        this.taskId = taskId;
        this.ticketId = ticketId;
        this.question = question;
        this.operatorContext = operatorContext;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getQuestion() {
        return question;
    }

    public OperatorContext getOperatorContext() {
        return operatorContext;
    }

    public String getContractName() {
        return CONTRACT_NAME;
    }
}
