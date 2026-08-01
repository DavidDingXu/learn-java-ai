# 双框架共存验证

Status: VERIFIED_CAPABILITY_ROUTING


## 已验证

- `FrameworkCoexistencePolicy` routes fixed business capabilities to Spring AI or LangChain4j.
- Unknown capabilities fail closed.
- The main reactor Enforcer rejects LangChain4j and AgentScope dependencies; the isolated labs reactor keeps its own BOMs and does not load both auto-configuration stacks into one Boot process.

## 验证命令

```bash
./mvnw -f labs/pom.xml verify
```

## 外部验证边界

The labs POM does not currently ban database or messaging dependencies. The project intentionally does not load both auto-configuration stacks into one Boot process; that option requires a separate compatibility test.
