package com.xiaoding.javaai.knowledge.document.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoding.javaai.knowledge.document.application.DocumentPublicationService;
import com.xiaoding.javaai.knowledge.document.application.DocumentUploadService;
import com.xiaoding.javaai.knowledge.document.application.PolicyDocumentChunker;
import com.xiaoding.javaai.knowledge.document.application.port.DocumentContentParser;
import com.xiaoding.javaai.knowledge.document.application.port.DocumentObjectStore;
import com.xiaoding.javaai.knowledge.document.web.JwtDocumentWriteIdentityFactory;
import com.xiaoding.javaai.knowledge.indexing.application.DocumentVersionIndexingService;
import com.xiaoding.javaai.knowledge.indexing.application.IndexTaskWorker;
import com.xiaoding.javaai.knowledge.indexing.application.port.DocumentVersionIndexer;
import com.xiaoding.javaai.knowledge.indexing.application.port.DocumentVersionTextSource;
import com.xiaoding.javaai.knowledge.indexing.application.port.IndexTaskQueue;
import com.xiaoding.javaai.knowledge.indexing.application.port.KnowledgeChunkIndexSink;
import com.xiaoding.javaai.knowledge.indexing.infrastructure.PgVectorKnowledgeChunkIndexSink;
import com.xiaoding.javaai.knowledge.indexing.infrastructure.PostgresIndexTaskQueue;
import com.xiaoding.javaai.knowledge.indexing.infrastructure.StoredDocumentVersionTextSource;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "java-ai.knowledge.ingestion.enabled", havingValue = "true")
public class KnowledgeIngestionConfiguration {

    @Bean
    DocumentObjectStore documentObjectStore(
            @Value("${java-ai.knowledge.object-store.local-root}") Path localRoot
    ) {
        return new LocalFileDocumentObjectStore(localRoot);
    }

    @Bean
    DocumentContentParser documentContentParser() {
        return new Utf8TextDocumentContentParser();
    }

    @Bean
    JdbcKnowledgeDocumentRepository knowledgeDocumentRepository(
            DataSource dataSource,
            PlatformTransactionManager transactionManager
    ) {
        return new JdbcKnowledgeDocumentRepository(
                new JdbcTemplate(dataSource), new TransactionTemplate(transactionManager)
        );
    }

    @Bean
    DocumentUploadService documentUploadService(
            JdbcKnowledgeDocumentRepository repository,
            DocumentObjectStore objectStore,
            DocumentContentParser parser
    ) {
        return new DocumentUploadService(repository, objectStore, parser);
    }

    @Bean
    DocumentPublicationService documentPublicationService(
            JdbcKnowledgeDocumentRepository repository,
            Clock clock
    ) {
        return new DocumentPublicationService(
                repository, repository, UUID::randomUUID, clock
        );
    }

    @Bean
    JwtDocumentWriteIdentityFactory jwtDocumentWriteIdentityFactory() {
        return new JwtDocumentWriteIdentityFactory();
    }

    @Bean
    IndexTaskQueue indexTaskQueue(DataSource dataSource) {
        return new PostgresIndexTaskQueue(dataSource);
    }

    @Bean
    DocumentVersionTextSource documentVersionTextSource(
            JdbcKnowledgeDocumentRepository repository,
            DocumentObjectStore objectStore,
            DocumentContentParser parser
    ) {
        return new StoredDocumentVersionTextSource(repository, objectStore, parser);
    }

    @Bean
    PolicyDocumentChunker policyDocumentChunker(
            @Value("${java-ai.knowledge.indexing.max-chunk-characters:1000}") int maxChunkCharacters
    ) {
        return new PolicyDocumentChunker(maxChunkCharacters);
    }

    @Bean
    KnowledgeChunkIndexSink knowledgeChunkIndexSink(
            DataSource dataSource,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        return new PgVectorKnowledgeChunkIndexSink(dataSource, objectMapper, clock);
    }

    @Bean
    DocumentVersionIndexer documentVersionIndexer(
            DocumentVersionTextSource source,
            PolicyDocumentChunker chunker,
            KnowledgeEmbeddingModel embeddingModel,
            KnowledgeChunkIndexSink sink,
            @Value("${java-ai.knowledge.indexing.chunk-policy-version:policy-chunk-v1}") String policyVersion,
            @Value("${java-ai.knowledge.indexing.embedding-batch-size:16}") int embeddingBatchSize
    ) {
        return new DocumentVersionIndexingService(
                source, chunker, embeddingModel, sink, policyVersion, embeddingBatchSize
        );
    }

    @Bean(destroyMethod = "shutdownNow")
    ScheduledExecutorService indexTaskLeaseRenewalExecutor() {
        return Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform()
                .daemon(true)
                .name("knowledge-index-lease-renewal")
                .factory());
    }

    @Bean
    IndexTaskWorker indexTaskWorker(
            IndexTaskQueue queue,
            DocumentVersionIndexer indexer,
            Clock clock,
            @Value("${java-ai.knowledge.indexing.worker-id}") String workerId,
            @Value("${java-ai.knowledge.indexing.lease-duration:45s}") String leaseDuration,
            @Value("${java-ai.knowledge.indexing.retry-delay:2m}") String retryDelay,
            @Value("${java-ai.knowledge.indexing.maximum-attempts:3}") int maximumAttempts,
            ScheduledExecutorService indexTaskLeaseRenewalExecutor
    ) {
        return new IndexTaskWorker(
                workerId, queue, indexer, clock::instant,
                DurationStyle.detectAndParse(leaseDuration),
                DurationStyle.detectAndParse(retryDelay),
                maximumAttempts,
                indexTaskLeaseRenewalExecutor
        );
    }
}
