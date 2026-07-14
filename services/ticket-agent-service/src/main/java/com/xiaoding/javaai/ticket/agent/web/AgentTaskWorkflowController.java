package com.xiaoding.javaai.ticket.agent.web;

import com.xiaoding.javaai.ticket.agent.application.AgentAuditTrail;
import com.xiaoding.javaai.ticket.agent.application.AgentRunAdmission;
import com.xiaoding.javaai.ticket.agent.application.ConfirmAgentAction;
import com.xiaoding.javaai.ticket.agent.application.ConfirmationActor;
import com.xiaoding.javaai.ticket.agent.application.RunAgentTask;
import com.xiaoding.javaai.ticket.security.ConfirmationActorFactory;
import com.xiaoding.javaai.ticket.task.AgentTask;
import com.xiaoding.javaai.ticket.task.AgentTaskAccessDeniedException;
import com.xiaoding.javaai.ticket.task.AgentTaskNotFoundException;
import com.xiaoding.javaai.ticket.task.AgentTaskRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agent/tasks")
public final class AgentTaskWorkflowController {

    private final AgentTaskRepository repository;
    private final RunAgentTask runAgentTask;
    private final ConfirmAgentAction confirmAgentAction;
    private final AgentAuditTrail auditTrail;
    private final ConfirmationActorFactory actorFactory;
    private final AgentRunAdmission runAdmission;

    public AgentTaskWorkflowController(
            AgentTaskRepository repository,
            RunAgentTask runAgentTask,
            ConfirmAgentAction confirmAgentAction,
            AgentAuditTrail auditTrail,
            ConfirmationActorFactory actorFactory,
            AgentRunAdmission runAdmission
    ) {
        this.repository = repository;
        this.runAgentTask = runAgentTask;
        this.confirmAgentAction = confirmAgentAction;
        this.auditTrail = auditTrail;
        this.actorFactory = actorFactory;
        this.runAdmission = runAdmission;
    }

    @GetMapping(path = "/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    AgentTaskView get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("taskId") String taskId
    ) {
        ConfirmationActor actor = actorFactory.create(jwt, "jdk8-crm");
        return AgentTaskView.from(ownedTask(taskId, actor));
    }

    @PostMapping(path = "/{taskId}/runs", produces = MediaType.APPLICATION_JSON_VALUE)
    AgentTaskView run(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("taskId") String taskId
    ) {
        return runAdmission.execute(() -> {
            ConfirmationActor actor = actorFactory.create(jwt, "ticket-agent-worker");
            ownedTask(taskId, actor);
            return AgentTaskView.from(runAgentTask.run(taskId));
        });
    }

    @PutMapping(
            path = "/{taskId}/confirmation",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    ConfirmationDecisionReceiptResponse confirm(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("taskId") String taskId,
            @RequestHeader("Idempotency-Key")
            @NotBlank @Size(min = 8, max = 128) String idempotencyKey,
            @Valid @RequestBody ConfirmToolActionWebRequest request
    ) {
        ConfirmationActor actor = actorFactory.create(jwt, "jdk8-crm");
        return ConfirmationDecisionReceiptResponse.from(confirmAgentAction.decide(
                taskId, actor, idempotencyKey, request.toApplication()));
    }

    @GetMapping(path = "/{taskId}/audit", produces = MediaType.APPLICATION_JSON_VALUE)
    List<AgentAuditEventResponse> audit(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("taskId") String taskId
    ) {
        ConfirmationActor actor = actorFactory.create(jwt, "jdk8-crm");
        ownedTask(taskId, actor);
        return auditTrail.findByTaskId(taskId).stream()
                .map(AgentAuditEventResponse::from)
                .toList();
    }

    private AgentTask ownedTask(String taskId, ConfirmationActor actor) {
        AgentTask task = repository.findById(taskId)
                .orElseThrow(() -> new AgentTaskNotFoundException(taskId));
        if (!task.identity().tenantId().equals(actor.tenantId())) {
            throw new AgentTaskAccessDeniedException("agent task belongs to a different tenant");
        }
        return task;
    }
}
