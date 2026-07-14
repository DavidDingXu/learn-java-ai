# Model Interaction Eval Runbook

## Contract Mode

Contract mode starts a loopback HTTP fixture and never calls a real model:

```bash
scripts/run-contract-eval.sh
```

PowerShell:

```powershell
.\scripts\run-contract-eval.ps1
```

## Live Mode

Live mode requires the model variables plus a short-lived delegated token in `JAVA_AI_EVAL_BEARER_TOKEN`. The token must contain the production-equivalent issuer, audience, actor, `knowledge:answer` scope, tenant and subject claims. Configure validation with `JAVA_AI_JWT_ISSUER` and either `JAVA_AI_JWT_JWK_SET_URI` or `JAVA_AI_DEV_JWT_HMAC_SECRET`.

The script binds Knowledge Service to `127.0.0.1`, but it does not disable authentication. Eval Runner sends the token as `Authorization: Bearer ...`, so the test crosses the same identity boundary as a normal HTTP request.

```bash
scripts/run-live-model-eval.sh
```

PowerShell:

```powershell
.\scripts\run-live-model-eval.ps1
```

## Reports

Both commands write JSON and Markdown. Reports must state mode, dataset version, model, code version, result counts, Token totals, latency, Trace ID and bad cases. API keys, bearer tokens and Provider base URLs must never be written.

## Retrieval Evaluation

Retrieval evaluation targets an already prepared Knowledge Service environment. The token in `JAVA_AI_RETRIEVAL_EVAL_BEARER_TOKEN` must carry `knowledge:eval` scope plus the tenant, subject, department, issuer, audience and actor claims accepted by that environment.

macOS or Linux:

```bash
JAVA_AI_RETRIEVAL_BASE_URL=https://knowledge-test.example.com \
JAVA_AI_RETRIEVAL_EVAL_BEARER_TOKEN=short-lived-token \
scripts/run-retrieval-eval.sh
```

PowerShell:

```powershell
$env:JAVA_AI_RETRIEVAL_BASE_URL = "https://knowledge-test.example.com"
$env:JAVA_AI_RETRIEVAL_EVAL_BEARER_TOKEN = "short-lived-token"
.\scripts\run-retrieval-eval.ps1
```

The environment must contain the versioned documents, ACLs and embeddings referenced by `datasets/retrieval/golden-set-v1.jsonl`. The runner records actual rankings, embedding model, Recall@K, HitRate@K, MRR, duplicate rate and p95 latency. It exits non-zero when a threshold fails or one run returns multiple embedding models.

## Agent Evaluation

Agent evaluation targets a deployed Ticket Agent Service and uses three short-lived delegated tokens instead of one broad test credential:

- `JAVA_AI_AGENT_CREATE_TOKEN`: actor `customer-bff`, scope `ticket:task:create`.
- `JAVA_AI_AGENT_RUN_TOKEN`: actor `ticket-agent-worker`, scope `ticket:task:run`.
- `JAVA_AI_AGENT_READ_TOKEN`: actor `jdk8-crm`, scope `ticket:task:read`.

The runner creates a task, runs it to a terminal or confirmation state, and reads the audit trail. It never calls the confirmation endpoint, so any write-execution audit event is a failed side-effect boundary.

```bash
JAVA_AI_AGENT_BASE_URL=https://ticket-agent-test.example.com \
JAVA_AI_AGENT_CREATE_TOKEN=create-token \
JAVA_AI_AGENT_RUN_TOKEN=run-token \
JAVA_AI_AGENT_READ_TOKEN=read-token \
scripts/run-agent-eval.sh
```

PowerShell uses the same variables with `scripts/run-agent-eval.ps1`. Reports contain task state, selected Tool, risk, required role, audit event types and latency. They never contain bearer tokens, Prompt bodies or business context.
