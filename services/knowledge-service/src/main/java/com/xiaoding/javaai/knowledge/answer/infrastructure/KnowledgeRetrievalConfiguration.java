package com.xiaoding.javaai.knowledge.answer.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.xiaoding.javaai.knowledge.answer.application.port.PolicyContextSource;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeRetrievalService;
import com.xiaoding.javaai.knowledge.retrieval.application.HybridKnowledgeRetrievalService;
import com.xiaoding.javaai.knowledge.retrieval.application.ReciprocalRankFusion;
import com.xiaoding.javaai.knowledge.retrieval.application.RetrievalPlan;
import com.xiaoding.javaai.knowledge.retrieval.application.StaticRetrievalPlanProvider;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeChunkSearchRepository;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeEmbeddingModel;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeRetriever;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeLexicalSearchRepository;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeQueryRewriter;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeReranker;
import com.xiaoding.javaai.knowledge.retrieval.application.port.RetrievalPlanProvider;
import com.xiaoding.javaai.knowledge.retrieval.infrastructure.PgVectorKnowledgeChunkSearchRepository;
import com.xiaoding.javaai.knowledge.retrieval.infrastructure.SpringAiKnowledgeEmbeddingModel;
import com.xiaoding.javaai.knowledge.retrieval.infrastructure.PostgresTrigramKnowledgeChunkSearchRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "java-ai.knowledge.context-source", havingValue = "retrieval")
class KnowledgeRetrievalConfiguration {

    private static final int DOCUMENT_CHUNK_EMBEDDING_DIMENSIONS = 1536;

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(DataSource.class)
    DataSource knowledgeDataSource(
            @Value("${java-ai.knowledge.postgres.jdbc-url}") String jdbcUrl,
            @Value("${java-ai.knowledge.postgres.username}") String username,
            @Value("${java-ai.knowledge.postgres.password}") String password,
            @Value("${java-ai.knowledge.postgres.maximum-pool-size:10}") int maximumPoolSize
    ) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("knowledge-postgres");
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(maximumPoolSize);
        config.setConnectionTimeout(3000);
        return new HikariDataSource(config);
    }

    @Bean
    KnowledgeEmbeddingModel knowledgeEmbeddingModel(
            EmbeddingModel embeddingModel
    ) {
        return new SpringAiKnowledgeEmbeddingModel(embeddingModel, DOCUMENT_CHUNK_EMBEDDING_DIMENSIONS);
    }

    @Bean
    KnowledgeChunkSearchRepository knowledgeChunkSearchRepository(
            DataSource dataSource,
            ObjectMapper objectMapper
    ) {
        return new PgVectorKnowledgeChunkSearchRepository(dataSource, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(
            name = "java-ai.knowledge.retrieval.mode",
            havingValue = "vector",
            matchIfMissing = true
    )
    KnowledgeRetriever knowledgeRetriever(
            KnowledgeEmbeddingModel embeddingModel,
            KnowledgeChunkSearchRepository searchRepository
    ) {
        return new KnowledgeRetrievalService(embeddingModel, searchRepository);
    }

    @Bean
    @ConditionalOnProperty(name = "java-ai.knowledge.retrieval.mode", havingValue = "hybrid")
    KnowledgeLexicalSearchRepository knowledgeLexicalSearchRepository(
            DataSource dataSource,
            ObjectMapper objectMapper
    ) {
        return new PostgresTrigramKnowledgeChunkSearchRepository(dataSource, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "java-ai.knowledge.retrieval.mode", havingValue = "hybrid")
    RetrievalPlanProvider retrievalPlanProvider(
            @Value("${java-ai.knowledge.retrieval.rewrite-query:false}") boolean rewriteQuery,
            @Value("${java-ai.knowledge.retrieval.lexical-search:true}") boolean lexicalSearch,
            @Value("${java-ai.knowledge.retrieval.rerank:false}") boolean rerank,
            @Value("${java-ai.knowledge.retrieval.candidate-k:20}") int candidateK
    ) {
        return new StaticRetrievalPlanProvider(new RetrievalPlan(
                rewriteQuery, lexicalSearch, rerank, candidateK
        ));
    }

    @Bean
    @ConditionalOnProperty(name = "java-ai.knowledge.retrieval.mode", havingValue = "hybrid")
    KnowledgeQueryRewriter knowledgeQueryRewriter() {
        return question -> {
            throw new IllegalStateException("query rewrite is enabled but no rewriter adapter is configured");
        };
    }

    @Bean
    @ConditionalOnProperty(name = "java-ai.knowledge.retrieval.mode", havingValue = "hybrid")
    KnowledgeReranker knowledgeReranker() {
        return (question, candidates, topK) -> {
            throw new IllegalStateException("rerank is enabled but no reranker adapter is configured");
        };
    }

    @Bean
    @ConditionalOnProperty(name = "java-ai.knowledge.retrieval.mode", havingValue = "hybrid")
    KnowledgeRetriever hybridKnowledgeRetriever(
            KnowledgeQueryRewriter queryRewriter,
            KnowledgeEmbeddingModel embeddingModel,
            KnowledgeChunkSearchRepository vectorSearch,
            KnowledgeLexicalSearchRepository lexicalSearch,
            KnowledgeReranker reranker,
            RetrievalPlanProvider planProvider
    ) {
        return new HybridKnowledgeRetrievalService(
                queryRewriter,
                embeddingModel,
                vectorSearch,
                lexicalSearch,
                reranker,
                planProvider,
                new ReciprocalRankFusion(60)
        );
    }

    @Bean
    PolicyContextSource retrievalPolicyContextSource(
            KnowledgeRetriever retriever,
            @Value("${java-ai.knowledge.retrieval.top-k:6}") int topK
    ) {
        return new RetrievalPolicyContextSource(retriever, topK);
    }
}
