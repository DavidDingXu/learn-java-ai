# 第 22 课 Customer Web 验证记录

Status: VERIFIED_LOCAL_BROWSER_AND_BUILD

## 覆盖范围

Customer Web 是 `apps/customer-web` 下的独立 React 应用。它只调用 Customer BFF，已经实现 POST SSE、引用展示、反馈、重新生成、工单升级、主动取消和桌面端/移动端响应式布局。

## 自动化验证

验证环境：macOS、Node.js 24.14.1、npm 11.11.0。

```bash
npm --prefix apps/customer-web ci
npm --prefix apps/customer-web run typecheck
npm --prefix apps/customer-web test
npm --prefix apps/customer-web run build
```

结果：3 个测试文件、9 个测试全部通过；TypeScript 类型检查和 Vite 生产构建通过。

测试覆盖任意 SSE 分块边界、没有尾部空行的最后一个事件、命名事件校验、HTTP 错误映射、浏览器 `fetch` 绑定、页面渲染、反馈校验、重试与升级状态，以及页面卸载时取消流式请求。

## 浏览器验证

使用脱敏咨询数据和本地 Customer BFF 兼容端点，分别在 1440 x 1000 与 390 x 844 视口完成检查。

- POST SSE 正常结束，页面渲染了两段增量文本和一条引用。
- 有帮助/没帮助按钮只在收到 `completed` 后出现。
- 提交 `NOT_HELPFUL` 前必须选择原因。
- 重新生成会创建新的回答记录，不覆盖上一条回答。
- 转人工接口返回任务回执，页面能够正确展示。
- 桌面端和移动端页面宽度与视口一致，没有横向溢出。
- 最终检查时，浏览器控制台没有错误或警告。
- 页面没有向客户展示 conversation、attempt 和 trace 等内部标识。

## 适用范围

本地兼容端点用于检查浏览器协议和页面状态，不用于评价模型质量或生产网络。公司 IdP、生产网关缓冲、分布式会话与限流存储、浏览器兼容策略和端到端容量，需要在目标环境继续验证。
