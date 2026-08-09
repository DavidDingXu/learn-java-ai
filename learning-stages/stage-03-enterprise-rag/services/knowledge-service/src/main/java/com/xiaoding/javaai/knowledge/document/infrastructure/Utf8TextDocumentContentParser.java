package com.xiaoding.javaai.knowledge.document.infrastructure;

import com.xiaoding.javaai.knowledge.document.application.DocumentParsingException;
import com.xiaoding.javaai.knowledge.document.application.ParsedDocument;
import com.xiaoding.javaai.knowledge.document.application.port.DocumentContentParser;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public final class Utf8TextDocumentContentParser implements DocumentContentParser {

    @Override
    public ParsedDocument parse(String mediaType, byte[] content) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
            return new ParsedDocument(text);
        } catch (CharacterCodingException error) {
            throw new DocumentParsingException("document is not valid UTF-8", error);
        }
    }
}
