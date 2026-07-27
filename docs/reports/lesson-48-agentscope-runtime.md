# Lesson 48 AgentScope Runtime Evidence

Status: VERIFIED_PERMISSION_MAPPING

Implementation commit: `5ee567645050a76bf54719a460b5c7069678572d`

## Verified

- `AgentScopeTicketRuntime` uses real `RuntimeContext`, `Toolkit` and `PermissionEngine` types.
- Query, update and customer export map to ALLOW, ASK and DENY.
- An unknown tool is rejected before permission evaluation.
- Trusted execution identity remains a business record; the decision also preserves the tool, rule source and readable reason.

## Verification

```bash
./mvnw -f labs/pom.xml -pl agentscope-lab test
```

## External Boundary

The lab maps runtime decisions only. Production confirmation persistence, business authorization and audit storage remain application responsibilities.
