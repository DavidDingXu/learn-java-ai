# Customer Web

面向 C 端客户的咨询应用。它通过 Customer BFF 完成流式回答、引用展示、反馈、重新生成和转人工，不直接访问 Knowledge Service 或 Ticket Agent Service。

## 本地运行界面

前端界面不需要模型密钥或 Java 服务：

```bash
cd apps/customer-web
npm ci
npm run dev
```

打开 `http://127.0.0.1:5173`。开发模式保留了短时令牌输入框，用于连接已部署的 Customer BFF 测试环境。本仓库不内置可冒充真实登录的固定令牌。

默认 `demo` Profile 可以直接启动 Customer BFF，但会关闭客户 JWT 和下游集成，所有咨询 API 都按安全默认值拒绝。它只用于验证 Spring 组装与健康检查，不是一条伪造身份和模型响应的前后端 Demo。

Windows PowerShell 使用相同的 `npm` 命令。

## 验证

```bash
npm run typecheck
npm test
npm run build
```

这三项检查不需要访问真实 Customer BFF。项目根目录的 `scripts/verify-unit.sh` 和 `scripts/verify-unit.ps1` 也会安装锁定依赖，并执行类型检查、测试和生产构建。

## 完整联调所需条件

完整咨询链路需要已部署的 Customer BFF、Knowledge Service、Ticket Agent 和公司身份平台。对应地址与身份参数写入 Customer BFF `application.yml` 的 `production` 配置段；真实密钥由部署密钥系统覆盖，不提交到仓库。

联调时由身份平台签发短时客户令牌，然后在“本地联调”中输入。Vite 默认将 `/api` 代理到 `http://localhost:8080`；如果目标 BFF 不在本机，再通过 Vite 的启动配置指向对应环境。

## 生产接入边界

- 生产部署默认让静态资源与 Customer BFF 同源；如由独立域名托管，需要在网关统一处理路由和跨域策略。
- 开发模式允许手工填入短时令牌。生产环境应接入公司的 OIDC/OAuth2 登录流程，不把访问令牌或客户端密钥写入前端包。
- 页面刷新后不会恢复历史会话，因为当前 BFF 没有提供会话查询接口；增加恢复能力时，应先补服务端接口和授权校验。
- Customer BFF 当前使用进程内会话与限流。多实例部署前需要替换为共享实现，并验证 TTL、并发更新和故障恢复。
- 高风险业务动作仍在客服终端确认，Customer Web 只负责咨询和发起工单升级。
