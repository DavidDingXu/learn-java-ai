package com.xiaoding.javaai.knowledge.document.infrastructure;

import com.xiaoding.javaai.knowledge.document.application.DocumentParsingException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Utf8TextDocumentContentParserTest {

    private final Utf8TextDocumentContentParser parser = new Utf8TextDocumentContentParser();

    @Test
    void parses_valid_utf8_text() {
        assertThat(parser.parse(
                "text/markdown", "# 退款政策".getBytes(StandardCharsets.UTF_8)
        ).text()).isEqualTo("# 退款政策");
    }

    @Test
    void rejects_invalid_utf8_instead_of_replacing_bytes() {
        assertThatThrownBy(() -> parser.parse("text/plain", new byte[]{(byte) 0xc3, (byte) 0x28}))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("UTF-8");
    }
}
