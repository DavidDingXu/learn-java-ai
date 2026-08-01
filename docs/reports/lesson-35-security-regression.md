# AI 安全回归验证

Status: VERIFIED_LOCAL_SECURITY_PIPELINE_SHARED_ENVIRONMENT_REQUIRED


## 已验证

- The 30-case versioned dataset covers confirmation bypass, request-body identity pollution and synthetic sensitive fragments in Agent audit details.
- Agent Eval checks forbidden execution events and forbidden audit fragments without copying the fragment into the report.
- Local tests cover the Knowledge JWT boundary, Ticket caller boundary, Tool Catalog, prompt partition and evaluator rules.
- External evaluation uses separate create, run and read tokens and has no confirmation capability.

## 本地验证

```bash
./mvnw -pl services/knowledge-service,services/ticket-agent-service,quality/eval-runner \
  -Dtest=KnowledgeJwtSecurityTest,TicketAgentJwtSecurityTest,BusinessToolCatalogTest,SpringAiTicketAgentPlannerPromptTest,AgentEvaluatorTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

## 外部验证边界

The local suite does not scan application logs or Trace data and does not run a two-tenant data-isolation scenario. The deployed `security-eval` command requires a dedicated test tenant and signed tokens. These checks verify the gate design, not complete production security coverage.
