package com.xiaoding.javaai.knowledge.retrieval.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;
import com.xiaoding.javaai.knowledge.retrieval.application.RetrievedKnowledgeChunk;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeLexicalSearchRepository;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public final class PostgresTrigramKnowledgeChunkSearchRepository implements KnowledgeLexicalSearchRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    public PostgresTrigramKnowledgeChunkSearchRepository(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<RetrievedKnowledgeChunk> search(
            String queryText,
            KnowledgeAccessScope scope,
            Instant effectiveAt,
            int topK
    ) {
        PostgresLexicalSearchQuery query = PostgresLexicalSearchQuery.create(
                queryText, scope, effectiveAt, topK
        );
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(query.sql())) {
            bind(statement, query.parameters());
            try (var resultSet = statement.executeQuery()) {
                return readChunks(resultSet);
            }
        } catch (Exception error) {
            throw new KnowledgeSearchPersistenceException("PostgreSQL trigram knowledge search failed", error);
        }
    }

    private static void bind(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index += 1) {
            int parameterIndex = index + 1;
            Object value = parameters.get(index);
            if (value instanceof String text) statement.setString(parameterIndex, text);
            else if (value instanceof Instant instant) {
                statement.setObject(parameterIndex, OffsetDateTime.ofInstant(instant, ZoneOffset.UTC));
            } else if (value instanceof Integer integer) statement.setInt(parameterIndex, integer);
            else throw new IllegalArgumentException("unsupported search parameter type " + value.getClass().getName());
        }
    }

    private List<RetrievedKnowledgeChunk> readChunks(ResultSet resultSet) throws Exception {
        List<RetrievedKnowledgeChunk> chunks = new ArrayList<>();
        while (resultSet.next()) {
            chunks.add(new RetrievedKnowledgeChunk(
                    resultSet.getString("chunk_id"),
                    new DocumentId(resultSet.getString("document_id")),
                    resultSet.getInt("version_number"),
                    objectMapper.readValue(resultSet.getString("heading_path"), STRING_LIST),
                    resultSet.getString("clause"),
                    resultSet.getString("content"),
                    resultSet.getDouble("score")
            ));
        }
        return List.copyOf(chunks);
    }
}
