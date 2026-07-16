# Customer Web

面向 C 端客户的独立 Web 应用边界。它负责承接流式回答、引用证据、反馈和工单升级体验，不进入 Maven reactor，也不提供高风险动作确认入口。

当前只定义产品与构建边界，尚未提供可运行的前端工程。正式实现应继续使用 BFF 暴露的回答、SSE、反馈和工单升级接口，不直接访问 Knowledge Service 或 Ticket Agent Service。
