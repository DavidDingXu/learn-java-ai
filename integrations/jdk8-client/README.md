# JDK8 Ticket Client

面向 Java 8 CRM/工单系统的独立 HTTP 客户端。该模块不引入 Spring AI，不继承 Java 21 构建配置，也不共享服务端 DTO。

## 职责

- 使用短时 Access Token 查询 Agent 任务；
- 携带幂等键提交员工确认或拒绝决定；
- 分离明确业务拒绝、HTTP 错误、传输失败和结果未知；
- 配置连接超时、响应超时与连接池上限。

客户端以 [`contracts/openapi/agent-task-v1.yaml`](../../contracts/openapi/agent-task-v1.yaml) 为协议依据。可信身份仍来自服务端验证的 Token，客户端不在请求体传递租户、用户或角色。

## 构建

需要同时包含 `java` 和 `javac` 的完整 JDK 8：

```bash
./mvnw -f integrations/jdk8-client/pom.xml verify
```

Windows PowerShell 使用 `.\mvnw.cmd -f integrations/jdk8-client/pom.xml verify`。项目约束测试会检查字节码目标、依赖边界与不确定结果映射。

集成到现有系统时，调用方需要提供 `AccessTokenProvider`，并根据下游 SLA 设置 `TicketAgentClientConfig`。非幂等写操作不能在超时后盲目重试；出现不确定结果时，必须使用原幂等键查询或对账。
