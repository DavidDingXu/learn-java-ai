# Lesson 47 Framework Coexistence Evidence

Status: VERIFIED_CAPABILITY_ROUTING

Implementation commit: `5ee567645050a76bf54719a460b5c7069678572d`

## Verified

- `FrameworkCoexistencePolicy` routes fixed business capabilities to Spring AI or LangChain4j.
- Unknown capabilities fail closed.
- Separate reactors and dependency allowlists prevent framework DTO and dependency leakage.

## Verification

```bash
./mvnw -f labs/pom.xml verify
```

## External Boundary

The project intentionally does not load both auto-configuration stacks into one Boot process. That option requires a separate compatibility test.
