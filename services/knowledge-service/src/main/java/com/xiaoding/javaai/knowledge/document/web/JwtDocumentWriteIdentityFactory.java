package com.xiaoding.javaai.knowledge.document.web;

import com.xiaoding.javaai.knowledge.document.domain.ActorId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtDocumentWriteIdentityFactory {
    DocumentWriteIdentity create(Jwt jwt) {
        return new DocumentWriteIdentity(
                new TenantId(jwt.getClaimAsString("tenantId")),
                new ActorId(jwt.getSubject())
        );
    }
}
