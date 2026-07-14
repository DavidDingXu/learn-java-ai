package com.xiaoding.javaai.ticket.agent.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("java-ai.agent")
class AgentDownstreamProperties {

    private String knowledgeBaseUrl;
    private String legacyToolBaseUrl;
    private Duration downstreamTimeout = Duration.ofSeconds(3);
    private DelegatedToken delegatedToken = new DelegatedToken();

    String getKnowledgeBaseUrl() {
        return knowledgeBaseUrl;
    }

    void setKnowledgeBaseUrl(String knowledgeBaseUrl) {
        this.knowledgeBaseUrl = knowledgeBaseUrl;
    }

    String getLegacyToolBaseUrl() {
        return legacyToolBaseUrl;
    }

    void setLegacyToolBaseUrl(String legacyToolBaseUrl) {
        this.legacyToolBaseUrl = legacyToolBaseUrl;
    }

    Duration getDownstreamTimeout() {
        return downstreamTimeout;
    }

    void setDownstreamTimeout(Duration downstreamTimeout) {
        this.downstreamTimeout = downstreamTimeout;
    }

    DelegatedToken getDelegatedToken() {
        return delegatedToken;
    }

    void setDelegatedToken(DelegatedToken delegatedToken) {
        this.delegatedToken = delegatedToken;
    }

    static class DelegatedToken {
        private String issuer;
        private String hmacSecret;
        private String actorId = "ticket-agent-service";
        private Duration ttl = Duration.ofMinutes(2);

        String getIssuer() {
            return issuer;
        }

        void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        String getHmacSecret() {
            return hmacSecret;
        }

        void setHmacSecret(String hmacSecret) {
            this.hmacSecret = hmacSecret;
        }

        String getActorId() {
            return actorId;
        }

        void setActorId(String actorId) {
            this.actorId = actorId;
        }

        Duration getTtl() {
            return ttl;
        }

        void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }
}
