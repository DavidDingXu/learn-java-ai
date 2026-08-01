# Customer BFF

客户咨询渠道的后端边界。该模块验证客户 JWT，换取最小权限的下游委托令牌，并组合 Knowledge Service 与 Ticket Agent Service 的 HTTP/SSE 协议。它不调用模型 SDK，也不复制知识、检索或工单领域规则。

## 主要能力

- 完整回答和具有命名事件的 SSE 回答；
- 按回答 attempt 记录反馈和重试；
- 将未解决问题作为不可变快照幂等升级为工单；
- 稳定映射下游超时、失败和 SSE 取消。

公开契约见 [`contracts/openapi/customer-bff-v1.yaml`](../../contracts/openapi/customer-bff-v1.yaml)。

## 运行与测试

```bash
./mvnw -pl apps/customer-bff test
./mvnw -pl apps/customer-bff spring-boot:run
```

默认 `demo` Profile 关闭客户 JWT、Token Exchange 和下游调用，咨询 API 按安全默认值拒绝。前端运行方式见 [`apps/customer-web/README.md`](../customer-web/README.md)。

使用 `-Dspring-boot.run.profiles=production` 可以启用客户 JWT、Token Exchange 和两个下游 HTTP 客户端。启动前需要替换本模块 `application.yml` 生产配置段中的占位值，完整清单见[运行配置](../../docs/runbooks/runtime-configuration.md)。

多实例部署前，需要将进程内会话与限流替换为共享实现，并验证 TTL、原子版本更新、故障恢复和网关超时。
