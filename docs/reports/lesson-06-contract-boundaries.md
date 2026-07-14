# Lesson 06 HTTP and Legacy Contract Evidence

## Status

`LOCAL_CONTRACTS_VERIFIED`

## Evidence boundary

- Input baseline commit: `f1a0989`.
- Three HTTP/OpenAPI 3.1 documents define the Knowledge, Agent Task and JDK8 legacy-tool boundaries.
- Two JSON Schema 2020-12 documents reject additional properties, including caller-supplied identity fields.
- Agent task creation and legacy tool actions require an `Idempotency-Key` header.
- The JDK8 client owns a Java 8-compatible `ToolActionCommand`; it has no dependency on Java 21 service types.

## Executable evidence

```bash
./mvnw -pl quality/eval-runner verify
./mvnw -f integrations/jdk8-client/pom.xml verify
```

The Eval Runner parses every OpenAPI document and validates positive and negative JSON fixtures against both schemas.

## Not claimed

These checks prove repository contracts and Java compatibility. They do not prove an external legacy system deployment or a production JWT trust relationship.
