# Lesson 38 Environment Modes Evidence

Status: VERIFIED_NO_DOCKER_CROSS_PLATFORM_BOUNDARIES

Implementation commit: `b8fd7c46e2329e48cdbdbedfcc58e8097afe306d`

## Verified

- `verify-local-lite` delegates to the full project verification with external services disabled.
- The unified gate auto-selects a full JDK 21+ and a separate full JDK8, then runs project, labs, Java 8 and workspace contracts.
- shared-dev requires explicit URLs and credentials; generic health verification refuses a missing environment.
- macOS/Linux shell and Windows PowerShell entry points have matching static contracts.
- Environment documentation states the evidence produced by local-lite, shared-dev and CI.

## Verification

```bash
bash scripts/verify-local-lite.sh
node --test scripts/verification-scripts.test.mjs
```

## External Boundary

PowerShell scripts are statically checked on macOS; release claims for Windows require a real Windows runner. External databases, IdP, object storage and business systems require company-managed environment reports.
