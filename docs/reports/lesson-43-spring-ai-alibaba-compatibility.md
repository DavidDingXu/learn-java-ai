# Spring AI Alibaba 兼容性验证

Status: VERIFIED_DEPENDENCY_ISOLATION


## 已验证

- Spring AI Alibaba 位于独立 Maven Reactor，不与 Spring Boot 4.1、Spring AI 2.0 主工程混合构建。
- 实验模块依赖白名单会阻止框架实验依赖进入正式服务模块。
- ADR 0003 记录迁移、转正和回滚条件。

## 验证命令

```bash
./mvnw -f labs/pom.xml -pl spring-ai-alibaba-lab verify
```

## 外部验证边界

Dependency isolation avoids claiming in-process compatibility. Promotion requires a target-service dependency tree and full regression evidence.
