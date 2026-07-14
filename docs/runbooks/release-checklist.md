# Release Checklist

## Required Gate

Run `scripts/release-gate.sh` on macOS/Linux or `scripts/release-gate.ps1` on Windows. The gate executes the root reactor, isolated framework labs, Java 8 client, Node contracts, column contracts when present, and a tracked/untracked secret scan.

## External Evidence

For a release that claims a deployed environment is ready, set `JAVA_AI_RELEASE_REQUIRE_EXTERNAL=1` and provide `JAVA_AI_EXTERNAL_BASE_URL`. The generic health smoke proves only one application health endpoint. Model, retrieval, Agent and security reports remain separate commands because they require different identities and datasets.

## Operational Review

- Database and schema migration owner is identified.
- Feature flags default to the intended release state.
- Model, embedding and rerank names are explicit.
- Secrets come from the approved secret system.
- Agent write tools have persistent idempotency and unknown-result recovery.
- Dashboards and alerts cover latency, error, token, capacity and uncertain execution.
- Rollback distinguishes application rollback, prompt/model rollback and data migration rollback.
- Runbooks contain on-call ownership and links to the exact reports used for the decision.
