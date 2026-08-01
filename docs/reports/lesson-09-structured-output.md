# 结构化输出验证

Status: VERIFIED

- Converter: Spring AI `BeanOutputConverter`
- Business validation: `KnowledgeAnswerValidator`

## 已验证

- 模型先按 JSON Schema 转成结构化对象，再验证引用是否属于本次授权知识上下文。
- 非拒答回答必须至少包含一个有效引用；拒答必须包含拒答原因。
- 回答长度、未知引用和“已经退款/已经建单”等未执行动作会被拒绝。
- `KnowledgeAnswerServiceTest` 覆盖未知引用，Provider fixture 覆盖 OpenAI 兼容响应结构到 DTO 的映射。
- 显式模型调用和 5 条 Golden Set 均通过结构化转换与业务校验。

## 替换边界

Schema 转换成功不等于业务正确。公司项目需要把金额、状态、枚举、引用、权限和副作用断言留在业务层，不能完全交给模型或框架 Converter。
