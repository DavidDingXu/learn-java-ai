# JDK8 工单客户端验证

Status: VERIFIED_JAVA8_HTTP_CLIENT_CONTRACT


## 已验证

- JDK8 模块使用完整的 Temurin 8 工具链编译，不依赖 Java 21 主工程。
- 客户端限制任务 ID 长度，使用连接池与连接/响应超时，并关闭 HTTP 自动重试。
- Bearer Token 由 `AccessTokenProvider` 提供，幂等键只放在 HTTP 请求头中。
- 读取链路的传输错误与写入结果未知使用不同异常类型。
- 结构化 API 异常保留 HTTP 状态、错误码和是否可重试，不暴露任意不可信响应正文。
- Agent Task OpenAPI 包含旧系统接入所需的任务读取、确认、审计、状态与错误响应。

## 验证命令

```bash
./mvnw -f integrations/jdk8-client/pom.xml verify
```

该独立 POM 要求 Maven 由 JDK8 运行。在 IDE 或 Maven 运行配置中选择 JDK8 即可，无需配置项目专用环境变量。

Result: 7 Java 8 tests passed in the latest focused verification run.

## 生产接入边界

Company integration must replace base URL discovery, TLS, proxy and token acquisition, and must confirm that any shared HTTP SDK also disables unsafe write retries. Callback delivery is optional and cannot replace task result queries.
