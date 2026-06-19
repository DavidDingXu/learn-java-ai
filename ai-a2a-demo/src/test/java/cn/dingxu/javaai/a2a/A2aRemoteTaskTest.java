package cn.dingxu.javaai.a2a;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class A2aRemoteTaskTest {

    @Test
    void taskCanPauseWithInputRequiredAndResumeAfterHumanInput() {
        HelpdeskAgentSkillServer server = HelpdeskAgentSkillServer.seeded();
        AgentTask task = server.createTask(new AgentTaskRequest(
                "ticket.advice",
                Map.of(
                        "ticketId", "T-2001",
                        "question", "高金额退款是否可以直接处理",
                        "approvalRequired", true
                )
        ));

        assertThat(task.state()).isEqualTo(TaskState.INPUT_REQUIRED);
        assertThat(task.statusMessage()).contains("human approval");
        assertThat(server.events(task.taskId())).extracting(TaskEvent::state)
                .containsExactly(TaskState.SUBMITTED, TaskState.WORKING, TaskState.INPUT_REQUIRED);

        AgentTask resumed = server.submitInput(task.taskId(), Map.of("approved", true));

        assertThat(resumed.state()).isEqualTo(TaskState.COMPLETED);
        assertThat(resumed.artifacts()).extracting(AgentArtifact::name)
                .containsExactly("ticket-advice");
        assertThat(server.events(task.taskId())).extracting(TaskEvent::state)
                .containsExactly(TaskState.SUBMITTED, TaskState.WORKING, TaskState.INPUT_REQUIRED, TaskState.COMPLETED);
    }

    @Test
    void pushNotificationConfigReceivesInputRequiredAndFinalState() {
        HelpdeskAgentSkillServer server = HelpdeskAgentSkillServer.seeded();
        InMemoryTaskCallback callback = new InMemoryTaskCallback();
        PushNotificationConfig pushConfig = new PushNotificationConfig(
                "https://callback.example.local/a2a/tasks",
                "secret-token"
        );
        AgentTask task = server.createTask(new AgentTaskRequest(
                "ticket.advice",
                Map.of(
                        "ticketId", "T-2002",
                        "question", "高金额退款是否可以直接处理",
                        "approvalRequired", true
                )
        ), callback, pushConfig);

        assertThat(task.state()).isEqualTo(TaskState.INPUT_REQUIRED);
        assertThat(callback.received()).hasSize(1);
        assertThat(callback.received().getFirst().state()).isEqualTo(TaskState.INPUT_REQUIRED);

        AgentTask completed = server.submitInput(task.taskId(), Map.of("approved", true));

        assertThat(completed.state()).isEqualTo(TaskState.COMPLETED);
        assertThat(callback.received()).extracting(AgentTask::state)
                .containsExactly(TaskState.INPUT_REQUIRED, TaskState.COMPLETED);
        assertThat(server.pushSubscriptions(task.taskId()))
                .extracting(PushNotificationConfig::url)
                .containsExactly("https://callback.example.local/a2a/tasks");
    }

    @Test
    void inputRequiredTaskFailsWhenHumanRejectsTheAction() {
        HelpdeskAgentSkillServer server = HelpdeskAgentSkillServer.seeded();
        AgentTask task = server.createTask(new AgentTaskRequest(
                "ticket.advice",
                Map.of(
                        "ticketId", "T-2003",
                        "question", "高金额退款是否可以直接处理",
                        "approvalRequired", true
                )
        ));

        AgentTask rejected = server.submitInput(task.taskId(), Map.of("approved", false));
        List<TaskEvent> events = server.events(task.taskId());

        assertThat(rejected.state()).isEqualTo(TaskState.FAILED);
        assertThat(rejected.statusMessage()).contains("human rejected");
        assertThat(events).extracting(TaskEvent::state)
                .containsExactly(TaskState.SUBMITTED, TaskState.WORKING, TaskState.INPUT_REQUIRED, TaskState.FAILED);
    }
}
