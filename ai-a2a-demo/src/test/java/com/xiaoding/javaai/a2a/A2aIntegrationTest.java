package com.xiaoding.javaai.a2a;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class A2aIntegrationTest {

    @Test
    void agentCardPublishesStableSkillsAndEndpoint() {
        HelpdeskAgentSkillServer server = HelpdeskAgentSkillServer.seeded();

        AgentCard card = server.agentCard();

        assertThat(card.name()).isEqualTo("helpdesk-agent");
        assertThat(card.endpoint()).isEqualTo("https://example.local/a2a/helpdesk");
        assertThat(card.skills()).extracting(AgentSkill::id)
                .containsExactly("ticket.advice");
        assertThat(card.skills().getFirst().inputSchema())
                .containsEntry("ticketId", "string")
                .containsEntry("question", "string");
    }

    @Test
    void clientCreatesTaskAndReceivesStatusEvents() {
        HelpdeskAgentSkillServer server = HelpdeskAgentSkillServer.seeded();
        A2aClient client = new A2aClient(server);
        AgentTaskRequest request = new AgentTaskRequest(
                "ticket.advice",
                Map.of("ticketId", "T-1001", "question", "客户申请退款但订单已发货怎么办")
        );

        AgentTask task = client.createTask(request);
        List<TaskEvent> events = client.streamTaskEvents(task.taskId());

        assertThat(task.state()).isEqualTo(TaskState.COMPLETED);
        assertThat(events).extracting(TaskEvent::state)
                .containsExactly(TaskState.SUBMITTED, TaskState.WORKING, TaskState.COMPLETED);
        assertThat(task.artifacts()).extracting(AgentArtifact::name)
                .containsExactly("ticket-advice");
    }

    @Test
    void serverRejectsUnknownSkillWithoutCreatingBusinessArtifact() {
        HelpdeskAgentSkillServer server = HelpdeskAgentSkillServer.seeded();

        AgentTask task = server.createTask(new AgentTaskRequest(
                "unknown.skill",
                Map.of("ticketId", "T-1001")
        ));

        assertThat(task.state()).isEqualTo(TaskState.FAILED);
        assertThat(task.artifacts()).isEmpty();
        assertThat(task.statusMessage()).contains("unknown skill");
    }

    @Test
    void callbackReceivesFinalTaskStateForAsyncConsumer() {
        HelpdeskAgentSkillServer server = HelpdeskAgentSkillServer.seeded();
        InMemoryTaskCallback callback = new InMemoryTaskCallback();

        AgentTask task = server.createTask(new AgentTaskRequest(
                "ticket.advice",
                Map.of("ticketId", "T-1002", "question", "高金额退款是否可以直接处理")
        ), callback);

        assertThat(task.state()).isEqualTo(TaskState.COMPLETED);
        assertThat(callback.received()).hasSize(1);
        assertThat(callback.received().getFirst().taskId()).isEqualTo(task.taskId());
        assertThat(callback.received().getFirst().state()).isEqualTo(TaskState.COMPLETED);
    }

    @Test
    void serverRejectsInvalidTaskInputAndKeepsFailureEvents() {
        HelpdeskAgentSkillServer server = HelpdeskAgentSkillServer.seeded();

        AgentTask task = server.createTask(new AgentTaskRequest(
                "ticket.advice",
                Map.of("question", "客户申请退款但缺少工单号")
        ));
        List<TaskEvent> events = server.events(task.taskId());

        assertThat(task.state()).isEqualTo(TaskState.FAILED);
        assertThat(task.artifacts()).isEmpty();
        assertThat(task.statusMessage()).contains("ticketId");
        assertThat(events).extracting(TaskEvent::state)
                .containsExactly(TaskState.SUBMITTED, TaskState.FAILED);
    }

    @Test
    void clientCanLoadTaskStateAfterCreation() {
        HelpdeskAgentSkillServer server = HelpdeskAgentSkillServer.seeded();
        A2aClient client = new A2aClient(server);

        AgentTask created = client.createTask(new AgentTaskRequest(
                "ticket.advice",
                Map.of("ticketId", "T-1003", "question", "客户申请退款但订单已发货怎么办")
        ));
        AgentTask loaded = client.getTask(created.taskId());

        assertThat(loaded.taskId()).isEqualTo(created.taskId());
        assertThat(loaded.state()).isEqualTo(TaskState.COMPLETED);
        assertThat(loaded.artifacts()).extracting(AgentArtifact::name)
                .containsExactly("ticket-advice");
    }
}
