package com.xiaoding.javaai.knowledge.security;

import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;
import org.springframework.security.core.Authentication;

public interface KnowledgeAccessScopeProvider {

    KnowledgeAccessScope currentScope(Authentication authentication);
}
