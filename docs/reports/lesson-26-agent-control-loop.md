# Lesson 26 Agent Control Loop Evidence

Status: VERIFIED_CONTROLLED_AGENT_STATE_MACHINE

Implementation commit: `44713c1a26c1e9a4d47354032db8c3e32d5e0b49`

## Verified

- Agent Task has explicit `ACCEPTED`, `RUNNING`, `WAITING_CONFIRMATION`, `EXECUTING`, `EXECUTION_UNCERTAIN`, `COMPLETED`, `REJECTED` and `FAILED` states.
- The orchestrator accepts only `USE_TOOL`, `FINISH` and `REFUSE` planner decisions and enforces a bounded step budget.
- Read tools record observations before the next planning step; write tools stop at a version-bound confirmation request.
- Planner metadata preserves model, usage and finish reason without leaking Spring AI response types into the application port.
- Audit records low-sensitive decision metadata and task transitions, not prompt bodies or customer content.

## Verification

```bash
./mvnw -pl services/ticket-agent-service \
  -Dtest=AgentTaskStateTest,TicketAgentOrchestratorTest,SpringAiTicketAgentPlannerPromptTest test
```

## Production Boundary

The `local-lite` repository and audit trail are in-memory. The `shared-dev` profile persists task snapshots, versions, confirmation decisions and audit events with JDBC/Flyway. Company deployment still must verify target-database concurrency, worker ownership, recovery and capacity before enabling production write tools.
