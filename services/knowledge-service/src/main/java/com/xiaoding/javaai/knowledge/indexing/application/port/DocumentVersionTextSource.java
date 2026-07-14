package com.xiaoding.javaai.knowledge.indexing.application.port;

import com.xiaoding.javaai.knowledge.indexing.application.ClaimedIndexTask;

public interface DocumentVersionTextSource {
    String loadText(ClaimedIndexTask task);
}
