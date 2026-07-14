# Environment Modes

| Mode | Purpose | Infrastructure | Evidence |
|---|---|---|---|
| local-lite | Daily development | In-memory task state and deterministic tests; external integrations disabled | Code, state, contract and failure semantics only |
| shared-dev | Real service and identity integration | Company-managed PostgreSQL, object storage, IdP, Knowledge and Legacy sandbox | Signed-token HTTP behavior and prepared test data |
| CI | Repeatable release gate | Real JDK21, JDK8 and pipeline-scoped infrastructure or remote dependencies | Build, tests, contracts, secret scan and selected external reports |

Run `scripts/verify-local-lite.sh` or `.ps1` for fast local regression. Use explicit external URLs and credentials for shared development; never infer them from developer defaults. A release that requires external evidence sets `JAVA_AI_RELEASE_REQUIRE_EXTERNAL=1` before `scripts/release-gate.sh` or `.ps1`.
