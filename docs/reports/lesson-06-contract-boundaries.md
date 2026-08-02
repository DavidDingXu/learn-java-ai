# HTTP 接口与 JDK8 客户端验证

## 状态

`LOCAL_CONTRACTS_VERIFIED`

## 证据边界

- 三份 HTTP/OpenAPI 3.1 文档分别定义 Knowledge、Agent Task 和 JDK8 旧系统 Tool 的接口边界。
- 两份 JSON Schema 2020-12 文档拒绝额外字段，包括调用方自行传入的身份字段。
- 创建 Agent Task 和调用旧系统 Tool 都必须提供 `Idempotency-Key` 请求头。
- JDK8 客户端维护自己的 Java 8 兼容 `ToolActionCommand`，不依赖 Java 21 服务类型。

## 可执行证据

```bash
./mvnw -pl quality/eval-runner verify
./mvnw -f integrations/jdk8-client/pom.xml verify
```

Eval Runner 会解析每份 OpenAPI 文档，并使用正向、负向 JSON 样例验证两套 Schema。

## 不包含的结论

这些检查验证仓库内的接口契约和 Java 兼容性，不代表已完成外部老系统部署或生产身份信任链联调。
