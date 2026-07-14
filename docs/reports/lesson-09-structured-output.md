# Lesson 09 Structured Output Evidence

Status: VERIFIED

- Implementation commit: `f5532160ae5018da237567e5855bd20bb7ce2123`
- Converter: Spring AI `BeanOutputConverter`
- Business validation: `KnowledgeAnswerValidator`

## Verified

- 模型先按 JSON Schema 转成结构化对象，再验证引用是否属于本次可信上下文。
- 非拒答回答必须至少包含一个有效引用；拒答必须包含拒答原因。
- 回答长度、未知引用和“已经退款/已经建单”等未执行动作会被拒绝。
- `KnowledgeAnswerServiceTest` 覆盖未知引用，Provider fixture 覆盖真实 OpenAI 兼容响应到结构化 DTO 的映射。
- 真实模型单次调用和 5 条 Golden Set 均通过结构化转换与业务校验。

## Replacement Boundary

Schema 转换成功不等于业务正确。公司项目需要把金额、状态、枚举、引用、权限和副作用断言留在业务层，不能完全交给模型或框架 Converter。
