# Lesson 39 Release Gate Evidence

Status: VERIFIED_LOCAL_RELEASE_GATE_EXTERNAL_EVIDENCE_OPTIONAL

Implementation commit: `b8fd7c46e2329e48cdbdbedfcc58e8097afe306d`

## Verified

- Cross-platform release scripts run the complete unit/build/contract boundary and scan tracked plus untracked non-ignored files for high-confidence secrets.
- `JAVA_AI_RELEASE_REQUIRE_EXTERNAL=1` requires an explicit deployed base URL and runs the external health smoke.
- Model, retrieval, Agent and security reports remain separate evidence with separate datasets and identities.
- Release documentation covers migration, feature flags, metrics, unknown-result recovery and application/model/index/database rollback.

## Unified Verification Result

- Node and workspace contracts: 113 passed.
- Knowledge Service: 85 tests passed.
- Ticket Agent Service: 50 tests passed.
- Customer BFF: 19 tests passed.
- Eval Runner: 24 tests passed.
- Main reactor total: 178 tests passed.
- Framework labs reactor built successfully.
- Independent Java 8 client: 6 tests passed.

## External Boundary

The default local gate does not claim a deployed model, database, vector index, object store or Legacy Tool is ready. A production release must require and retain the product-specific external reports used by the Go/No-Go decision.
