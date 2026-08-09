# Customer Web

面向 C 端客户的咨询应用。它通过 Customer BFF 完成流式回答、引用展示、反馈、重新生成和转人工，不直接访问 Knowledge Service 或 Ticket Agent Service。

## 本地运行界面

前端界面不需要模型密钥或 Java 服务：

```bash
cd apps/customer-web
npm ci
npm run dev
```

打开 `http://127.0.0.1:5173`。本地 Customer BFF 默认使用固定身份，令牌输入框可以留空；连接启用了公司鉴权的测试环境时，再输入身份平台签发的短时令牌。

Windows PowerShell 使用相同的 `npm` 命令。

## 完整联调所需条件

本地完整咨询链路需要同时启动 Customer BFF、Knowledge Service 和 Ticket Agent，不要求身份平台。三个服务默认使用 localhost 地址和固定身份，可以直接联调。

接入公司测试环境时，再把 BFF 切换到 JWT 与 OAuth2 委托，由身份平台签发短时客户令牌。Vite 默认将 `/api` 代理到 `http://localhost:8080`；如果目标 BFF 不在本机，再通过 Vite 的启动配置指向对应环境。

## 生产接入边界

- 生产部署默认让静态资源与 Customer BFF 同源；如由独立域名托管，需要在网关统一处理路由和跨域策略。
- 开发模式允许手工填入短时令牌。生产环境应接入公司的 OIDC/OAuth2 登录流程，不把访问令牌或客户端密钥写入前端包。
- 页面刷新后不会恢复历史会话，因为当前 BFF 没有提供会话查询接口；增加恢复能力时，应先补服务端接口和授权校验。
- Customer BFF 当前使用进程内会话与限流。多实例部署前需要替换为共享实现，并验证 TTL、并发更新和故障恢复。
- 高风险业务动作仍在客服终端确认，Customer Web 只负责咨询和发起工单升级。
