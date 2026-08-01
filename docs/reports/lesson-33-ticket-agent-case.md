# 工单 Agent 业务链验证

Status: VERIFIED_COMPONENT_CHAIN_SHARED_ENVIRONMENT_REQUIRED


## 已验证

- A handoff task can run through bounded planning, read-tool observation and a write-tool confirmation boundary.
- Real Spring AI 2.0 model planning selected `QUERY_KNOWLEDGE` with structured metadata in the live smoke.
- Knowledge and Legacy HTTP adapters enforce target-specific audience, scope, timeout and response mapping.
- Delegated JWT uses `departmentIds` consistently and validates allowed actors in Knowledge and Ticket services.
- The public Agent Task OpenAPI supports create, read, run, confirm and audit operations.
- Model, read-tool, confirmation and remote execution failures have distinct task and HTTP behavior.

## 验证命令

```bash
./mvnw -pl services/ticket-agent-service,services/knowledge-service \
  -Dtest=TicketAgentOrchestratorTest,HttpKnowledgeReadToolExecutorTest,HttpLegacyWriteToolExecutorTest,TicketAgentJwtSecurityTest,KnowledgeJwtSecurityTest,AgentTaskWorkflowControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

./mvnw -pl services/ticket-agent-service \
  -Dtest=TicketAgentLiveModelSmokeIT \
  -Dspring.config.additional-location=file:../../config/application.yml \
  -Djava-ai.agent-smoke.report-path=target/agent-live-model-smoke.md \
  test
```

## 外部验证边界

The live smoke proves real model planning, not a deployed production chain. Real IdP, persistent task store, prepared Knowledge index and JDK8 business sandbox still require shared-environment verification with signed short-lived tokens.
