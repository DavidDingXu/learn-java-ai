# Spring AI Alibaba 兼容性验证

Status: VERIFIED_DEPENDENCY_ISOLATION


## 已验证

- Spring AI Alibaba runs in a separate reactor from the Spring Boot 4.1 and Spring AI 2.0 mainline.
- The lab dependency allowlist prevents experimental framework dependencies from entering production modules.
- ADR 0003 records migration, promotion and rollback conditions.

## 验证命令

```bash
./mvnw -f labs/pom.xml -pl spring-ai-alibaba-lab verify
```

## 外部验证边界

Dependency isolation avoids claiming in-process compatibility. Promotion requires a target-service dependency tree and full regression evidence.
