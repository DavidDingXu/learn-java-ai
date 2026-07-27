# Lesson 40 DashScope Provider Replacement Evidence

Status: VERIFIED_ISOLATED_LAB

Implementation commit: `5ee567645050a76bf54719a460b5c7069678572d`

## Verified

- `DashScopeProviderAdapter` maps the stable business request to real Spring AI Alibaba `DashScopeChatOptions`.
- The adapter returns a project-owned `ProviderAnswer` instead of exposing Spring AI response types.
- Tests cover prompt/option mapping, configured-model fallback and the absence of an API-key field in the business response.

## Verification

```bash
./mvnw -f labs/pom.xml -pl spring-ai-alibaba-lab test
```

## External Boundary

The lab does not normalize usage or finish reasons and does not map provider exceptions into stable application errors. It also does not claim a live DashScope call. Production adoption still requires those adapter tests plus real credentials, model regression, rate-limit, content-filter and usage verification.
