package com.xiaoding.javaai.knowledge.document.application.port;

import com.xiaoding.javaai.knowledge.document.application.ParsedDocument;

public interface DocumentContentParser {
    ParsedDocument parse(String mediaType, byte[] content);
}
