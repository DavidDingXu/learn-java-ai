package com.xiaoding.javaai.knowledge.security;

import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;
import org.springframework.security.core.Authentication;

import java.util.List;

public final class FixedKnowledgeAccessScopeProvider implements KnowledgeAccessScopeProvider {

    private static final KnowledgeAccessScope LOCAL_READER = new KnowledgeAccessScope(
            new TenantId("tenant-a"), "local-user", List.of("support")
    );

    @Override
    public KnowledgeAccessScope currentScope(Authentication authentication) {
        return LOCAL_READER;
    }
}
