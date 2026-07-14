# Lesson 43 Spring AI Alibaba Compatibility Evidence

Status: VERIFIED_DEPENDENCY_ISOLATION

Implementation commit: `5ee567645050a76bf54719a460b5c7069678572d`

## Verified

- Spring AI Alibaba runs in a separate reactor from the Spring Boot 4.1 and Spring AI 2.0 mainline.
- The lab dependency allowlist prevents experimental framework dependencies from entering production modules.
- ADR 0003 records migration, promotion and rollback conditions.

## Verification

```bash
./mvnw -f labs/pom.xml -pl spring-ai-alibaba-lab verify
```

## External Boundary

Dependency isolation avoids claiming in-process compatibility. Promotion requires a target-service dependency tree and full regression evidence.
