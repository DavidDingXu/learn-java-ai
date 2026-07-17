package com.xiaoding.javaai.knowledge.document.web;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBufferLimitException;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeDocumentExceptionHandlerTest {

    @Test
    void maps_an_oversized_multipart_buffer_to_http_413() {
        var response = new KnowledgeDocumentExceptionHandler()
                .tooLarge(new DataBufferLimitException("Exceeded limit on max bytes to buffer"));

        assertThat(response.getStatusCode().value()).isEqualTo(413);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("KNOWLEDGE_DOCUMENT_TOO_LARGE");
    }
}
