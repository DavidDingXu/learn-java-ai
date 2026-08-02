package com.xiaoding.javaai.knowledge.answer.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoding.javaai.knowledge.answer.application.port.PolicyContextSource;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeRetriever;
import com.xiaoding.javaai.knowledge.retrieval.application.HybridKnowledgeRetrievalService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeRetrievalConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(KnowledgeRetrievalConfiguration.class)
            .withPropertyValues(
                    "java-ai.knowledge.mode=postgres-rag",
                    "java-ai.knowledge.retrieval.mode=hybrid",
                    "java-ai.knowledge.retrieval.lexical-search=true",
                    "java-ai.knowledge.retrieval.candidate-k=20",
                    "java-ai.knowledge.retrieval.top-k=6"
            )
            .withBean(DataSource.class, StubDataSource::new)
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(EmbeddingModel.class, StubEmbeddingModel::new);

    @Test
    void wires_the_retriever_and_policy_context_without_replacing_an_external_data_source() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DataSource.class);
            assertThat(context).hasSingleBean(KnowledgeRetriever.class);
            assertThat(context.getBean(KnowledgeRetriever.class))
                    .isInstanceOf(HybridKnowledgeRetrievalService.class);
            assertThat(context).hasSingleBean(PolicyContextSource.class);
            assertThat(context.getBean(DataSource.class)).isInstanceOf(StubDataSource.class);
        });
    }

    private static final class StubEmbeddingModel implements EmbeddingModel {
        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public float[] embed(Document document) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class StubDataSource implements DataSource {
        @Override public Connection getConnection() throws SQLException { throw new UnsupportedOperationException(); }
        @Override public Connection getConnection(String username, String password) throws SQLException { throw new UnsupportedOperationException(); }
        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter out) { }
        @Override public void setLoginTimeout(int seconds) { }
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() { return Logger.getGlobal(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("not a wrapper"); }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }
}
