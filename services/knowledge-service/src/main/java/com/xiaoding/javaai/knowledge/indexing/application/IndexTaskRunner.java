package com.xiaoding.javaai.knowledge.indexing.application;

import com.xiaoding.javaai.knowledge.document.domain.TenantId;

public interface IndexTaskRunner {

    IndexTaskRunResult runOnce();

    IndexTaskRunResult runOnce(TenantId tenantId);
}
