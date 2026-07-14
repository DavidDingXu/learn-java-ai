package com.xiaoding.javaai.knowledge.indexing.application.port;

import com.xiaoding.javaai.knowledge.indexing.application.ClaimedIndexTask;

public interface DocumentVersionIndexer {
    void index(ClaimedIndexTask task);
}
