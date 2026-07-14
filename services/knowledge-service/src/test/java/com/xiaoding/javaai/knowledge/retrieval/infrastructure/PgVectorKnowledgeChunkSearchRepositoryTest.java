package com.xiaoding.javaai.knowledge.retrieval.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PgVectorKnowledgeChunkSearchRepositoryTest {

    @Test
    void binds_the_search_scope_and_maps_retrieved_chunks() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("chunk_id")).thenReturn("chunk-1");
        when(resultSet.getString("document_id")).thenReturn("refund-policy");
        when(resultSet.getInt("version_number")).thenReturn(2);
        when(resultSet.getString("heading_path")).thenReturn("[\"售后政策\",\"退款\"]");
        when(resultSet.getString("clause")).thenReturn("第十条");
        when(resultSet.getString("content")).thenReturn("退款审核通过后原路退回。");
        when(resultSet.getDouble("score")).thenReturn(0.91d);

        PgVectorKnowledgeChunkSearchRepository repository =
                new PgVectorKnowledgeChunkSearchRepository(dataSource, new ObjectMapper());
        Instant now = Instant.parse("2026-07-12T10:00:00Z");

        var result = repository.search(
                new float[]{0.1f, 0.2f},
                "embedding-v1",
                new KnowledgeAccessScope(new TenantId("tenant-a"), "user-1", List.of("support")),
                now,
                5
        );

        assertThat(result).singleElement().satisfies(chunk -> {
            assertThat(chunk.chunkId()).isEqualTo("chunk-1");
            assertThat(chunk.documentId()).isEqualTo(new DocumentId("refund-policy"));
            assertThat(chunk.documentVersion()).isEqualTo(2);
            assertThat(chunk.headingPath()).containsExactly("售后政策", "退款");
            assertThat(chunk.clause()).isEqualTo("第十条");
            assertThat(chunk.score()).isEqualTo(0.91d);
        });

        var order = inOrder(statement);
        order.verify(statement).setString(1, "[0.1,0.2]");
        order.verify(statement).setString(2, "tenant-a");
        order.verify(statement).setString(3, "embedding-v1");
        order.verify(statement).setObject(4, OffsetDateTime.ofInstant(now, ZoneOffset.UTC));
        order.verify(statement).setObject(5, OffsetDateTime.ofInstant(now, ZoneOffset.UTC));
        order.verify(statement).setString(6, "user-1");
        order.verify(statement).setString(7, "support");
        order.verify(statement).setString(8, "tenant-a");
        order.verify(statement).setString(9, "[0.1,0.2]");
        order.verify(statement).setInt(10, 5);
    }
}
