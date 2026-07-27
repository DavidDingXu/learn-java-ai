# Lesson 47 Framework Coexistence Evidence

Status: VERIFIED_CAPABILITY_ROUTING

Implementation commit: `5ee567645050a76bf54719a460b5c7069678572d`

## Verified

- `FrameworkCoexistencePolicy` routes fixed business capabilities to Spring AI or LangChain4j.
- Unknown capabilities fail closed.
- The main reactor Enforcer rejects LangChain4j and AgentScope dependencies; the isolated labs reactor keeps its own BOMs and does not load both auto-configuration stacks into one Boot process.

## Verification

```bash
./mvnw -f labs/pom.xml verify
```

## External Boundary

The labs POM does not currently ban database or messaging dependencies. The project intentionally does not load both auto-configuration stacks into one Boot process; that option requires a separate compatibility test.
