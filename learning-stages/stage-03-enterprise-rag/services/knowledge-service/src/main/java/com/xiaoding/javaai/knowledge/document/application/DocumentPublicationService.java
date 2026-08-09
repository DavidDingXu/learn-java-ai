package com.xiaoding.javaai.knowledge.document.application;

import com.xiaoding.javaai.knowledge.document.application.port.KnowledgeDocumentRepository;
import com.xiaoding.javaai.knowledge.document.application.port.PublishedDocumentStore;
import com.xiaoding.javaai.knowledge.indexing.domain.IndexTask;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

public final class DocumentPublicationService {

    private final KnowledgeDocumentRepository repository;
    private final PublishedDocumentStore publicationStore;
    private final Supplier<UUID> taskIds;
    private final Clock clock;

    public DocumentPublicationService(
            KnowledgeDocumentRepository repository,
            PublishedDocumentStore publicationStore,
            Supplier<UUID> taskIds,
            Clock clock
    ) {
        this.repository = repository;
        this.publicationStore = publicationStore;
        this.taskIds = taskIds;
        this.clock = clock;
    }

    public PublishedDocument publish(PublishDocumentCommand command) {
        Instant publishedAt = clock.instant();
        if (command.effectiveFrom().isAfter(publishedAt)) {
            throw new IllegalArgumentException(
                    "future publication is not supported; trigger publication when effectiveFrom is reached"
            );
        }
        var document = repository.find(command.tenantId(), command.documentId())
                .orElseThrow(() -> new DocumentNotFoundException(command.tenantId(), command.documentId()));
        document.publishVersion(
                command.expectedRevision(),
                command.versionNumber(),
                command.effectiveFrom(),
                command.effectiveUntil()
        );
        IndexTask task = IndexTask.pending(
                taskIds.get(), command.tenantId(), command.documentId(), command.versionNumber(), publishedAt
        );
        publicationStore.savePublication(document, command.acl(), task, command.actorId(), publishedAt);
        return new PublishedDocument(
                command.tenantId(), command.documentId(), command.versionNumber(), document.revision(), task.taskId()
        );
    }
}
