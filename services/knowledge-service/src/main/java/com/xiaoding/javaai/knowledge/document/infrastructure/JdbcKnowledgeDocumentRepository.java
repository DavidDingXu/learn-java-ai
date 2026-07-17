package com.xiaoding.javaai.knowledge.document.infrastructure;

import com.xiaoding.javaai.knowledge.document.application.DocumentAclGrant;
import com.xiaoding.javaai.knowledge.document.application.port.KnowledgeDocumentRepository;
import com.xiaoding.javaai.knowledge.document.application.port.PublishedDocumentStore;
import com.xiaoding.javaai.knowledge.document.domain.ActorId;
import com.xiaoding.javaai.knowledge.document.domain.ContentHash;
import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.DocumentRevisionConflictException;
import com.xiaoding.javaai.knowledge.document.domain.DocumentVersion;
import com.xiaoding.javaai.knowledge.document.domain.DocumentVersionStatus;
import com.xiaoding.javaai.knowledge.document.domain.KnowledgeDocument;
import com.xiaoding.javaai.knowledge.document.domain.ObjectKey;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.indexing.application.IndexTaskType;
import com.xiaoding.javaai.knowledge.indexing.domain.IndexTask;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class JdbcKnowledgeDocumentRepository
        implements KnowledgeDocumentRepository, PublishedDocumentStore {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    public JdbcKnowledgeDocumentRepository(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = java.util.Objects.requireNonNull(jdbc, "jdbc must not be null");
        this.transactions = java.util.Objects.requireNonNull(transactions, "transactions must not be null");
    }

    @Override
    public Optional<KnowledgeDocument> find(TenantId tenantId, DocumentId documentId) {
        return transactions.execute(status -> findInsideTransaction(tenantId, documentId));
    }

    @Override
    public void save(KnowledgeDocument document) {
        try {
            transactions.executeWithoutResult(status -> saveInsideTransaction(document));
        } catch (DocumentRevisionConflictException error) {
            throw error;
        } catch (DataAccessException error) {
            throw new DocumentPersistenceException("failed to save document " + document.id().value(), error);
        }
    }

    @Override
    public void savePublication(
            KnowledgeDocument document,
            List<DocumentAclGrant> acl,
            IndexTask indexTask,
            ActorId actorId,
            Instant publishedAt
    ) {
        try {
            transactions.executeWithoutResult(status -> {
                updateRevision(document, publishedAt);
                for (DocumentVersion version : document.versions()) {
                    int updated = jdbc.update("""
                                    update document_version
                                       set status = ?, effective_from = ?, effective_until = ?
                                     where tenant_id = ? and document_id = ? and version_number = ?
                                    """,
                            version.status().name(), timestamp(version.effectiveFrom()),
                            timestamp(version.effectiveUntil()), document.tenantId().value(),
                            document.id().value(), version.number());
                    if (updated != 1) {
                        throw new DocumentPersistenceException(
                                "document version disappeared during publication", null);
                    }
                }
                int publicationRecorded = jdbc.update("""
                                update document_version
                                   set published_by = ?, published_at = ?
                                 where tenant_id = ? and document_id = ? and version_number = ?
                                """,
                        actorId.value(), timestamp(publishedAt), document.tenantId().value(),
                        document.id().value(), indexTask.documentVersion());
                if (publicationRecorded != 1) {
                    throw new DocumentPersistenceException("published document version disappeared", null);
                }
                jdbc.update("delete from document_acl where tenant_id = ? and document_id = ?",
                        document.tenantId().value(), document.id().value());
                for (DocumentAclGrant grant : acl) {
                    jdbc.update("""
                                    insert into document_acl (
                                        tenant_id, document_id, subject_type, subject_id, permission, created_at
                                    ) values (?, ?, ?, ?, 'READ', ?)
                                    """,
                            document.tenantId().value(), document.id().value(), grant.subjectType().name(),
                            grant.subjectId(), timestamp(publishedAt));
                }
                jdbc.update("""
                                insert into index_task (
                                    task_id, tenant_id, document_id, version_number, task_type, status,
                                    attempts, next_attempt_at, created_at, updated_at
                                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                                """,
                        indexTask.taskId(), indexTask.tenantId().value(), indexTask.documentId().value(),
                        indexTask.documentVersion(), IndexTaskType.REINDEX_DOCUMENT_VERSION.name(),
                        indexTask.status().name(), indexTask.attempts(), timestamp(indexTask.nextAttemptAt()),
                        timestamp(indexTask.createdAt()), timestamp(indexTask.updatedAt()));
            });
        } catch (DocumentRevisionConflictException | DocumentPersistenceException error) {
            throw error;
        } catch (DataAccessException error) {
            throw new DocumentPersistenceException(
                    "failed to publish document " + document.id().value(), error);
        }
    }

    private Optional<KnowledgeDocument> findInsideTransaction(TenantId tenantId, DocumentId documentId) {
        List<DocumentRow> documents = jdbc.query("""
                        select title, revision, created_by, created_at
                          from knowledge_document
                         where tenant_id = ? and document_id = ?
                        """,
                (resultSet, rowNumber) -> new DocumentRow(
                        resultSet.getString("title"),
                        resultSet.getLong("revision"),
                        new ActorId(resultSet.getString("created_by")),
                        instant(resultSet, "created_at")
                ),
                tenantId.value(), documentId.value());
        if (documents.isEmpty()) return Optional.empty();

        List<DocumentVersion> versions = jdbc.query("""
                        select version_number, content_hash, object_key, media_type, status,
                               effective_from, effective_until, created_by, created_at
                          from document_version
                         where tenant_id = ? and document_id = ?
                         order by version_number
                        """,
                (resultSet, rowNumber) -> DocumentVersion.restore(
                        resultSet.getInt("version_number"),
                        new ContentHash(resultSet.getString("content_hash")),
                        new ObjectKey(resultSet.getString("object_key")),
                        resultSet.getString("media_type"),
                        new ActorId(resultSet.getString("created_by")),
                        instant(resultSet, "created_at"),
                        DocumentVersionStatus.valueOf(resultSet.getString("status")),
                        nullableInstant(resultSet, "effective_from"),
                        nullableInstant(resultSet, "effective_until")
                ),
                tenantId.value(), documentId.value());
        DocumentRow row = documents.getFirst();
        return Optional.of(KnowledgeDocument.restore(
                documentId, tenantId, row.title(), row.createdBy(), row.createdAt(), row.revision(), versions
        ));
    }

    private void saveInsideTransaction(KnowledgeDocument document) {
        List<Long> revisions = jdbc.query(
                "select revision from knowledge_document where tenant_id = ? and document_id = ?",
                (resultSet, rowNumber) -> resultSet.getLong("revision"),
                document.tenantId().value(), document.id().value());
        Instant updatedAt = document.versions().getLast().createdAt();
        if (revisions.isEmpty()) {
            if (document.revision() != 1) {
                throw new DocumentRevisionConflictException(0, document.revision());
            }
            jdbc.update("""
                            insert into knowledge_document (
                                tenant_id, document_id, title, status, revision,
                                created_by, created_at, updated_at
                            ) values (?, ?, ?, 'ACTIVE', ?, ?, ?, ?)
                            """,
                    document.tenantId().value(), document.id().value(), document.title(), document.revision(),
                    document.createdBy().value(), timestamp(document.createdAt()), timestamp(updatedAt));
        } else {
            updateRevision(document, updatedAt);
        }

        Set<Integer> storedVersions = new HashSet<>(jdbc.query(
                "select version_number from document_version where tenant_id = ? and document_id = ?",
                (resultSet, rowNumber) -> resultSet.getInt("version_number"),
                document.tenantId().value(), document.id().value()));
        for (DocumentVersion version : document.versions()) {
            if (storedVersions.contains(version.number())) continue;
            jdbc.update("""
                            insert into document_version (
                                tenant_id, document_id, version_number, content_hash, object_key, media_type,
                                status, effective_from, effective_until, created_by, created_at
                            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    document.tenantId().value(), document.id().value(), version.number(),
                    version.contentHash().value(), version.objectKey().value(), version.mediaType(),
                    version.status().name(), timestamp(version.effectiveFrom()), timestamp(version.effectiveUntil()),
                    version.createdBy().value(), timestamp(version.createdAt()));
        }
    }

    private void updateRevision(KnowledgeDocument document, Instant updatedAt) {
        long expectedRevision = document.revision() - 1;
        int updated = jdbc.update("""
                        update knowledge_document
                           set revision = ?, updated_at = ?
                         where tenant_id = ? and document_id = ? and revision = ?
                        """,
                document.revision(), timestamp(updatedAt), document.tenantId().value(),
                document.id().value(), expectedRevision);
        if (updated == 1) return;
        long actual = jdbc.query(
                        "select revision from knowledge_document where tenant_id = ? and document_id = ?",
                        (resultSet, rowNumber) -> resultSet.getLong("revision"),
                        document.tenantId().value(), document.id().value())
                .stream().findFirst().orElse(-1L);
        throw new DocumentRevisionConflictException(expectedRevision, actual);
    }

    private static OffsetDateTime timestamp(Instant instant) {
        return instant == null ? null : OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        Instant value = nullableInstant(resultSet, column);
        if (value == null) throw new SQLException(column + " must not be null");
        return value;
    }

    private static Instant nullableInstant(ResultSet resultSet, String column) throws SQLException {
        Object value = resultSet.getObject(column);
        if (value == null) return null;
        if (value instanceof OffsetDateTime offsetDateTime) return offsetDateTime.toInstant();
        if (value instanceof Timestamp timestamp) return timestamp.toInstant();
        throw new SQLException("unsupported timestamp value " + value.getClass().getName());
    }

    private record DocumentRow(String title, long revision, ActorId createdBy, Instant createdAt) {
    }
}
