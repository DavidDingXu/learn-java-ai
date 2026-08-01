# AgentScope Runtime 适配验证

Status: VERIFIED_PERMISSION_MAPPING


## 已验证

- `AgentScopeTicketRuntime` uses real `RuntimeContext`, `Toolkit` and `PermissionEngine` types.
- Query, update and customer export map to ALLOW, ASK and DENY.
- An unknown tool is rejected before permission evaluation.
- Trusted execution identity remains a business record; the decision also preserves the tool, rule source and readable reason.

## 验证命令

```bash
./mvnw -f labs/pom.xml -pl agentscope-lab test
```

## 外部验证边界

The lab maps runtime decisions only. Production confirmation persistence, business authorization and audit storage remain application responsibilities.
