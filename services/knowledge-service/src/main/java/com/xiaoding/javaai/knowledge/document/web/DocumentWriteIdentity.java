package com.xiaoding.javaai.knowledge.document.web;

import com.xiaoding.javaai.knowledge.document.domain.ActorId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;

record DocumentWriteIdentity(TenantId tenantId, ActorId actorId) {
}
