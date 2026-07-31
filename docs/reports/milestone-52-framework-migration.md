# 里程碑 52：框架迁移与协议互操作

Status: VERIFIED_LOCAL_FRAMEWORK_BOUNDARIES

## 验证范围

- Spring AI 2.0 remains the production mainline.
- Spring AI Alibaba, LangChain4j, AgentScope and protocol interoperability stay in an isolated labs reactor.
- Lab-local ports preserve the mainline business semantics without importing service internals; shared evaluation rules compare provider, retrieval, graph, AI Services, Tool, runtime, MCP and A2A boundaries.
- MCP Java SDK `2.0.0` and A2A Java SDK `1.1.0.Final` exercise real local protocol interoperability instead of only local schema objects.
- Ticket Agent runtime persists tasks, audit events and confirmation decisions with PostgreSQL/Flyway; in-memory adapters remain test-only.
- Dataset gates include 50 retrieval cases, 30 Agent cases and 30 synthetic security cases.
- ADR 0003 defines promotion and rollback conditions.

## 验证命令

```bash
./mvnw -f labs/pom.xml verify
bash scripts/release-gate.sh
```

## 适用范围

Local tests prove framework API usage, dependency isolation, MCP/A2A client interoperability and business boundary behavior. Live DashScope, production MCP/A2A authentication, target PostgreSQL capacity and company infrastructure require credentials and target-environment evidence before production promotion.
