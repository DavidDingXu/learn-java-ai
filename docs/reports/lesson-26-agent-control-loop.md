# Agent 受控运行验证

Status: VERIFIED_CONTROLLED_AGENT_STATE_MACHINE


## 已验证

- Agent Task has explicit `ACCEPTED`, `RUNNING`, `WAITING_CONFIRMATION`, `EXECUTING`, `EXECUTION_UNCERTAIN`, `COMPLETED`, `REJECTED` and `FAILED` states.
- The orchestrator accepts only `USE_TOOL`, `FINISH` and `REFUSE` planner decisions and enforces a bounded step budget.
- Read tools record observations before the next planning step; write tools stop at a version-bound confirmation request.
- Planner metadata preserves model, usage and finish reason without leaking Spring AI response types into the application port.
- Audit records low-sensitive decision metadata and task transitions, not prompt bodies or customer content.

## 验证命令

```bash
./mvnw -pl services/ticket-agent-service \
  -Dtest=AgentTaskStateTest,TicketAgentOrchestratorTest,SpringAiTicketAgentPlannerPromptTest test
```

## 生产接入边界

The test configuration keeps repository and audit state in memory. The runtime configuration persists task snapshots, versions, confirmation decisions and audit events with JDBC/Flyway. Company deployment still must verify target-database concurrency, worker ownership, recovery and capacity before enabling production write tools.
