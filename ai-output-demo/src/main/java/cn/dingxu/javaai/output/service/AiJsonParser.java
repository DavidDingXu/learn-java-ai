package cn.dingxu.javaai.output.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class AiJsonParser {

    private final ObjectMapper objectMapper;

    public AiJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T parseStrict(String rawJson, Class<T> targetType) {
        try {
            return objectMapper.readValue(rawJson, targetType);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("AI output is not valid JSON for " + targetType.getSimpleName(), e);
        }
    }

    public String buildRepairPrompt(String rawOutput, String targetSchema) {
        return """
                下面是一段 AI 输出，它没有严格符合目标 JSON 结构。
                请只返回修复后的 JSON，不要解释，不要使用 Markdown 代码块。

                目标结构：
                %s

                原始输出：
                %s
                """.formatted(targetSchema, rawOutput);
    }
}
