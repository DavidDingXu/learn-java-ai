package com.xiaoding.javaai.knowledge.document.application.port;

import com.xiaoding.javaai.knowledge.document.application.DocumentAclGrant;
import com.xiaoding.javaai.knowledge.document.domain.KnowledgeDocument;
import com.xiaoding.javaai.knowledge.document.domain.ActorId;
import com.xiaoding.javaai.knowledge.indexing.domain.IndexTask;

import java.time.Instant;
import java.util.List;

public interface PublishedDocumentStore {
    void savePublication(
            KnowledgeDocument document,
            List<DocumentAclGrant> acl,
            IndexTask indexTask,
            ActorId actorId,
            Instant publishedAt
    );
}
