# Customer Web

面向 C 端客户的咨询应用。它通过 Customer BFF 完成流式回答、引用展示、反馈、重新生成和转人工，不直接访问 Knowledge Service 或 Ticket Agent Service。

## 本地运行

项目统一读取根目录 `.env`。先按根 README 启动三个 Java 服务，再在项目根目录生成短时客户令牌：

```bash
node scripts/generate-development-jwt.mjs --profile customer
```

安装依赖并启动前端：

```bash
cd apps/customer-web
npm ci
npm run dev
```

打开 `http://127.0.0.1:5173`，在“本地联调”中填入刚生成的令牌。Vite 将 `/api` 代理到 `http://localhost:8080`，浏览器不需要额外配置跨域。

Windows PowerShell 使用相同的 Node 命令和 `npm` 命令即可。

## 验证

```bash
npm run typecheck
npm test
npm run build
```

根目录的 `scripts/verify-build.sh` 和 `scripts/verify-build.ps1` 也会安装锁定依赖，并执行以上三项检查。

## 接入边界

- 生产部署默认让静态资源与 Customer BFF 同源；如由独立域名托管，需要在网关统一处理路由和跨域策略。
- 开发模式允许手工填入短时令牌。生产环境应接入公司的 OIDC/OAuth2 登录流程，不把访问令牌或客户端密钥写入前端包。
- 页面刷新后不会恢复历史会话，因为当前 BFF 没有提供会话查询接口；增加恢复能力时，应先补服务端接口和授权校验。
- Customer BFF 当前使用进程内会话与限流。多实例部署前需要替换为共享实现，并验证 TTL、并发更新和故障恢复。
- 高风险业务动作仍在客服终端确认，Customer Web 只负责咨询和发起工单升级。
