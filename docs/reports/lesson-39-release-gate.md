# Lesson 39 Release Gate Evidence

Status: VERIFIED_GATE_BEHAVIOR_FINAL_BASELINE_REQUIRES_RERUN

## Verified

- Cross-platform release scripts run the complete unit/build/contract boundary and scan tracked plus untracked non-ignored files for high-confidence secrets.
- The optional aggregate gate runs external health smoke only when the deployment pipeline explicitly supplies a target URL; local demo setup does not require this value.
- Model, retrieval, Agent and security reports remain separate evidence with separate datasets and identities.
- Release documentation covers migration, feature flags, metrics, unknown-result recovery and application/model/index/database rollback.

## Verification Result Boundary

The repository has changed since the earlier fixed-count report. The final release commit must rerun the direct Maven/Node checks and, when full repository coverage is needed, the optional aggregate gate. Exact test counts and the final commit belong in the regenerated release report; older counts are not carried forward as current evidence.

## External Boundary

The default local gate does not claim a deployed model, database, vector index, object store or Legacy Tool is ready. A production release must require and retain the product-specific external reports used by the Go/No-Go decision.
