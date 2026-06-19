package cn.dingxu.javaai.a2a;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HelpdeskAgentSkillServer {

    private static final String TICKET_ADVICE_SKILL = "ticket.advice";

    private final AgentCard card;
    private final Map<String, AgentTask> tasks = new LinkedHashMap<>();
    private final Map<String, List<TaskEvent>> events = new LinkedHashMap<>();
    private final Map<String, TaskCallback> callbacks = new LinkedHashMap<>();
    private final Map<String, List<PushNotificationConfig>> pushSubscriptions = new LinkedHashMap<>();

    public HelpdeskAgentSkillServer(AgentCard card) {
        this.card = card;
    }

    public static HelpdeskAgentSkillServer seeded() {
        AgentSkill skill = new AgentSkill(
                TICKET_ADVICE_SKILL,
                "工单处理建议",
                "根据工单问题生成处理建议，并返回可追踪任务产物。",
                Map.of("ticketId", "string", "question", "string")
        );
        return new HelpdeskAgentSkillServer(new AgentCard(
                "helpdesk-agent",
                "企业工单 AI 助手，面向外部 Agent 暴露工单处理建议 Skill。",
                "https://example.local/a2a/helpdesk",
                List.of(skill)
        ));
    }

    public AgentCard agentCard() {
        return card;
    }

    public AgentTask createTask(AgentTaskRequest request) {
        return createTask(request, null);
    }

    public synchronized AgentTask createTask(AgentTaskRequest request, TaskCallback callback) {
        return createTask(request, callback, null);
    }

    public synchronized AgentTask createTask(AgentTaskRequest request,
                                             TaskCallback callback,
                                             PushNotificationConfig pushConfig) {
        String taskId = "task-" + UUID.randomUUID();
        if (callback != null) {
            callbacks.put(taskId, callback);
        }
        if (pushConfig != null) {
            pushSubscriptions.computeIfAbsent(taskId, ignored -> new ArrayList<>()).add(pushConfig);
        }
        appendEvent(taskId, TaskState.SUBMITTED, "task submitted");

        AgentSkill skill = findSkill(request.skillId());
        if (skill == null) {
            AgentTask failed = AgentTask.failed(taskId, request.skillId(), "unknown skill: " + request.skillId());
            tasks.put(taskId, failed);
            appendEvent(taskId, TaskState.FAILED, failed.statusMessage());
            notifyCallback(callback, failed);
            return failed;
        }

        String invalidInputMessage = validateInput(skill, request.input());
        if (!invalidInputMessage.isBlank()) {
            AgentTask failed = AgentTask.failed(taskId, request.skillId(), invalidInputMessage);
            tasks.put(taskId, failed);
            appendEvent(taskId, TaskState.FAILED, failed.statusMessage());
            notifyCallback(callback, failed);
            return failed;
        }

        appendEvent(taskId, TaskState.WORKING, "helpdesk agent is composing advice");
        if (requiresApproval(request.input())) {
            AgentTask inputRequired = AgentTask.inputRequired(
                    taskId,
                    request.skillId(),
                    "human approval required before completing task"
            );
            tasks.put(taskId, inputRequired);
            appendEvent(taskId, TaskState.INPUT_REQUIRED, inputRequired.statusMessage());
            notifyCallback(callback, inputRequired);
            return inputRequired;
        }

        AgentArtifact artifact = composeTicketAdvice(request.input());
        AgentTask completed = AgentTask.completed(
                taskId,
                request.skillId(),
                "ticket advice completed",
                List.of(artifact)
        );
        tasks.put(taskId, completed);
        appendEvent(taskId, TaskState.COMPLETED, completed.statusMessage());
        notifyCallback(callback, completed);
        return completed;
    }

    public synchronized AgentTask submitInput(String taskId, Map<String, Object> input) {
        AgentTask existing = tasks.get(taskId);
        if (existing == null) {
            return AgentTask.failed(taskId, "unknown", "task not found: " + taskId);
        }
        if (existing.state() != TaskState.INPUT_REQUIRED) {
            return existing;
        }

        boolean approved = Boolean.parseBoolean(String.valueOf(input.getOrDefault("approved", false)));
        if (!approved) {
            AgentTask failed = AgentTask.failed(taskId, existing.skillId(), "human rejected the requested action");
            tasks.put(taskId, failed);
            appendEvent(taskId, TaskState.FAILED, failed.statusMessage());
            notifyCallback(callbacks.get(taskId), failed);
            return failed;
        }

        AgentArtifact artifact = new AgentArtifact("ticket-advice", "application/json", Map.of(
                "ticketId", taskId,
                "risk", "HIGH",
                "advice", "人工确认后可以生成处理建议，但写操作仍需走业务系统审批。",
                "requiresHumanApproval", true
        ));
        AgentTask completed = AgentTask.completed(
                taskId,
                existing.skillId(),
                "ticket advice completed after human approval",
                List.of(artifact)
        );
        tasks.put(taskId, completed);
        appendEvent(taskId, TaskState.COMPLETED, completed.statusMessage());
        notifyCallback(callbacks.get(taskId), completed);
        return completed;
    }

    public synchronized AgentTask getTask(String taskId) {
        AgentTask task = tasks.get(taskId);
        if (task == null) {
            return AgentTask.failed(taskId, "unknown", "task not found: " + taskId);
        }
        return task;
    }

    public synchronized List<TaskEvent> events(String taskId) {
        return List.copyOf(events.getOrDefault(taskId, List.of()));
    }

    public synchronized List<PushNotificationConfig> pushSubscriptions(String taskId) {
        return List.copyOf(pushSubscriptions.getOrDefault(taskId, List.of()));
    }

    private AgentSkill findSkill(String skillId) {
        return card.skills().stream()
                .filter(skill -> skill.id().equals(skillId))
                .findFirst()
                .orElse(null);
    }

    private String validateInput(AgentSkill skill, Map<String, Object> input) {
        for (String field : skill.inputSchema().keySet()) {
            Object value = input.get(field);
            if (value == null || String.valueOf(value).isBlank()) {
                return "invalid input: " + field + " must not be blank";
            }
        }
        return "";
    }

    private void appendEvent(String taskId, TaskState state, String message) {
        events.computeIfAbsent(taskId, ignored -> new ArrayList<>())
                .add(TaskEvent.of(taskId, state, message));
    }

    private boolean requiresApproval(Map<String, Object> input) {
        return Boolean.parseBoolean(String.valueOf(input.getOrDefault("approvalRequired", false)));
    }

    private AgentArtifact composeTicketAdvice(Map<String, Object> input) {
        String ticketId = String.valueOf(input.getOrDefault("ticketId", ""));
        String question = String.valueOf(input.getOrDefault("question", ""));
        String advice = question.contains("高金额")
                ? "高金额退款必须转人工复核，AI 只能给出建议，不能直接执行退款。"
                : "订单已发货时，先核对物流状态，再根据拒收、退回或签收状态选择处理路径。";
        return new AgentArtifact("ticket-advice", "application/json", Map.of(
                "ticketId", ticketId,
                "risk", question.contains("高金额") ? "HIGH" : "MEDIUM",
                "advice", advice,
                "requiresHumanApproval", true
        ));
    }

    private void notifyCallback(TaskCallback callback, AgentTask task) {
        if (callback != null
                && (task.state() == TaskState.INPUT_REQUIRED
                || task.state() == TaskState.COMPLETED
                || task.state() == TaskState.FAILED)) {
            callback.onCompleted(task);
        }
    }
}
