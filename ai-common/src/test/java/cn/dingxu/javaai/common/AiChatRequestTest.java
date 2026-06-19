package cn.dingxu.javaai.common;

import cn.dingxu.javaai.common.model.AiChatRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AiChatRequestTest {

    @Test
    void rejectsBlankMessage() {
        assertThrows(IllegalArgumentException.class, () -> new AiChatRequest("u1001", " "));
    }
}
