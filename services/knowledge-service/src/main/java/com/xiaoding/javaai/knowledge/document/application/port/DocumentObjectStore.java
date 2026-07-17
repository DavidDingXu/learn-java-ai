package com.xiaoding.javaai.knowledge.document.application.port;

import com.xiaoding.javaai.knowledge.document.domain.ObjectKey;

public interface DocumentObjectStore {
    void put(ObjectKey key, String mediaType, byte[] content);

    byte[] get(ObjectKey key);
}
