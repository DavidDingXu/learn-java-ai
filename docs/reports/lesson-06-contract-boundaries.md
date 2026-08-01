# HTTP 接口与 JDK8 客户端验证

## 状态

`LOCAL_CONTRACTS_VERIFIED`

## 证据边界

- Three HTTP/OpenAPI 3.1 documents define the Knowledge, Agent Task and JDK8 legacy-tool boundaries.
- Two JSON Schema 2020-12 documents reject additional properties, including caller-supplied identity fields.
- Agent task creation and legacy tool actions require an `Idempotency-Key` header.
- The JDK8 client owns a Java 8-compatible `ToolActionCommand`; it has no dependency on Java 21 service types.

## 可执行证据

```bash
./mvnw -pl quality/eval-runner verify
./mvnw -f integrations/jdk8-client/pom.xml verify
```

The Eval Runner parses every OpenAPI document and validates positive and negative JSON fixtures against both schemas.

## 不包含的结论

These checks prove repository contracts and Java compatibility. They do not prove an external legacy system deployment or a production JWT trust relationship.
