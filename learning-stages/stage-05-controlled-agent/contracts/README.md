# 阶段 05 接口契约

- `openapi/agent-task-v1.yaml`：创建、运行、查询、确认与审计。
- `openapi/legacy-tool-v1.yaml`：受控写 Tool 的远程执行与回执。
- 两份 JSON Schema 与 fixtures：任务输入和 Tool 命令的正反例。

Knowledge Service 和 Customer BFF 的接口没有变化，分别沿用阶段 03、04。阶段目录只展示 Agent 新增的接口，避免读者把未变化的复制文件误认为本篇改动。
