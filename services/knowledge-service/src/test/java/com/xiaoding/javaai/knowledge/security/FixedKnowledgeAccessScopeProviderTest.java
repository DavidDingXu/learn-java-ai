package com.xiaoding.javaai.knowledge.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FixedKnowledgeAccessScopeProviderTest {

    @Test
    void returnsTheFixedLocalReaderIdentityWithoutAuthentication() {
        var scope = new FixedKnowledgeAccessScopeProvider().currentScope(null);

        assertThat(scope.tenantId().value()).isEqualTo("tenant-a");
        assertThat(scope.subjectId()).isEqualTo("local-user");
        assertThat(scope.departmentIds()).containsExactly("support");
    }

    @Test
    void identityConfigurationRejectsUnknownModes() {
        var configuration = new KnowledgeIdentityConfiguration();

        assertThat(configuration.knowledgeAccessScopeProvider("fixed"))
                .isInstanceOf(FixedKnowledgeAccessScopeProvider.class);
        assertThat(configuration.knowledgeAccessScopeProvider("jwt"))
                .isInstanceOf(JwtKnowledgeAccessScopeProvider.class);
        assertThatThrownBy(() -> configuration.knowledgeAccessScopeProvider("disabled"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fixed or jwt");
    }
}
