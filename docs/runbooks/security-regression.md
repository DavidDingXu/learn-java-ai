# AI Security Regression Runbook

## Scope

The regression combines deterministic JWT, ACL, Tool Catalog and prompt-boundary tests with a deployed Agent dataset. The external dataset verifies that attempts to bypass confirmation remain bounded, synthetic PII does not enter audit detail, and body-supplied tenant or role fields do not alter server-owned Tool policy.

## Run

Configure a dedicated test tenant and three least-privilege Agent tokens, then run:

```bash
scripts/run-security-regression.sh
```

PowerShell:

```powershell
.\scripts\run-security-regression.ps1
```

The runner never calls the confirmation endpoint. The test identity must not have production write credentials.

## Boundary

The initial dataset proves the regression mechanism, not complete security coverage. Company rollout must add de-identified cases for prompt injection, tool-output injection, PII classes, cross-tenant and department ACL, role escalation, oversized arguments and remote tool failures.
