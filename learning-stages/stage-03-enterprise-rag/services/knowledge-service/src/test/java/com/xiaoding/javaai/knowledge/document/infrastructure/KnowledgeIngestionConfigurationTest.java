package com.xiaoding.javaai.knowledge.document.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoding.javaai.knowledge.document.application.DocumentPublicationService;
import com.xiaoding.javaai.knowledge.document.application.DocumentUploadService;
import com.xiaoding.javaai.knowledge.indexing.application.IndexTaskWorker;
import com.xiaoding.javaai.knowledge.indexing.application.port.DocumentVersionIndexer;
import com.xiaoding.javaai.knowledge.indexing.application.port.DocumentVersionTextSource;
import com.xiaoding.javaai.knowledge.indexing.application.port.IndexTaskQueue;
import com.xiaoding.javaai.knowledge.indexing.application.port.KnowledgeChunkIndexSink;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeEmbeddingModel;
import com.xiaoding.javaai.knowledge.retrieval.infrastructure.DeterministicHashEmbeddingModel;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeIngestionConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestDependencies.class, KnowledgeIngestionConfiguration.class)
            .withPropertyValues(
                    "java-ai.knowledge.mode=postgres-rag",
                    "java-ai.knowledge.object-store.local-root=target/test-objects",
                    "java-ai.knowledge.indexing.worker-id=test-worker"
            );

    @Test
    void assembles_the_upload_publication_and_index_worker_graph() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(DocumentUploadService.class);
            assertThat(context).hasSingleBean(DocumentPublicationService.class);
            assertThat(context).hasSingleBean(JdbcKnowledgeDocumentRepository.class);
            assertThat(context).hasSingleBean(DocumentVersionTextSource.class);
            assertThat(context).hasSingleBean(KnowledgeChunkIndexSink.class);
            assertThat(context).hasSingleBean(IndexTaskQueue.class);
            assertThat(context).hasSingleBean(DocumentVersionIndexer.class);
            assertThat(context).hasSingleBean(IndexTaskWorker.class);
            assertThat(context).hasSingleBean(Clock.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestDependencies {
        @Bean
        DataSource dataSource() {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:ingestion-config;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
            return dataSource;
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        KnowledgeEmbeddingModel knowledgeEmbeddingModel() {
            return new DeterministicHashEmbeddingModel(16);
        }

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }
}
