package cn.dingxu.javaai.rag.service;

import java.util.List;

public interface DocumentChunker {

    List<DocumentChunk> chunk(ParsedDocument document);
}
