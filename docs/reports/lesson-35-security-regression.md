# Lesson 35 AI Security Regression Evidence

Status: VERIFIED_LOCAL_SECURITY_PIPELINE_SHARED_ENVIRONMENT_REQUIRED

Implementation commit: `b8fd7c46e2329e48cdbdbedfcc58e8097afe306d`

## Verified

- The versioned security dataset covers confirmation bypass, synthetic PII in audit and request-body tenant/role pollution.
- Agent Eval checks forbidden execution events and forbidden audit fragments without copying the fragment into the report.
- The cross-platform security command combines Knowledge JWT, Ticket JWT, Tool Catalog, prompt-boundary and evaluator tests before external HTTP evaluation.
- External evaluation uses separate create, run and read tokens and has no confirmation capability.

## Local Verification

```bash
./mvnw -pl services/knowledge-service,services/ticket-agent-service,quality/eval-runner \
  -Dtest=KnowledgeJwtSecurityTest,TicketAgentJwtSecurityTest,BusinessToolCatalogTest,SpringAiTicketAgentPlannerPromptTest,AgentEvaluatorTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

## External Boundary

The deployed `security-eval` command requires a dedicated test tenant and signed tokens. The 30 teaching cases prove the gate design and core boundary assertions, not complete production security coverage.
