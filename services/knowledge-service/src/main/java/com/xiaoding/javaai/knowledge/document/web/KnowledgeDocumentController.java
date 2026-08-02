package com.xiaoding.javaai.knowledge.document.web;

import com.xiaoding.javaai.knowledge.document.application.DocumentPublicationService;
import com.xiaoding.javaai.knowledge.document.application.DocumentUploadService;
import com.xiaoding.javaai.knowledge.document.application.PublishDocumentCommand;
import com.xiaoding.javaai.knowledge.document.application.UploadDocumentCommand;
import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.ActorId;
import com.xiaoding.javaai.knowledge.security.KnowledgeAccessScopeProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;

@Validated
@RestController
@RequestMapping("/api/v1/knowledge/documents")
@ConditionalOnProperty(name = "java-ai.knowledge.mode", havingValue = "postgres-rag")
public final class KnowledgeDocumentController {

    private static final int MAX_BUFFERED_UPLOAD_BYTES = 5 * 1024 * 1024 + 1;

    private final DocumentUploadService uploadService;
    private final DocumentPublicationService publicationService;
    private final KnowledgeAccessScopeProvider accessScopeProvider;
    private final Clock clock;

    public KnowledgeDocumentController(
            DocumentUploadService uploadService,
            DocumentPublicationService publicationService,
            KnowledgeAccessScopeProvider accessScopeProvider,
            Clock clock
    ) {
        this.uploadService = uploadService;
        this.publicationService = publicationService;
        this.accessScopeProvider = accessScopeProvider;
        this.clock = clock;
    }

    @PostMapping(
            path = "/{documentId}/versions",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    Mono<ResponseEntity<UploadedDocumentResponse>> upload(
            Authentication authentication,
            @PathVariable
            @Size(max = 160)
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*") String documentId,
            @Valid @RequestPart("metadata") UploadDocumentMetadata metadata,
            @RequestPart("file") FilePart file
    ) {
        var scope = accessScopeProvider.currentScope(authentication);
        return DataBufferUtils.join(file.content(), MAX_BUFFERED_UPLOAD_BYTES)
                .map(buffer -> {
                    byte[] content = new byte[buffer.readableByteCount()];
                    try {
                        buffer.read(content);
                    } finally {
                        DataBufferUtils.release(buffer);
                    }
                    MediaType contentType = file.headers().getContentType();
                    String mediaType = contentType == null
                            ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                            : contentType.getType() + "/" + contentType.getSubtype();
                    return new UploadDocumentCommand(
                            new DocumentId(documentId), scope.tenantId(), metadata.title(), new ActorId(scope.subjectId()),
                            file.filename(), mediaType, content, metadata.expectedRevision(), clock.instant()
                    );
                })
                .publishOn(Schedulers.boundedElastic())
                .map(uploadService::upload)
                .map(UploadedDocumentResponse::from)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @PostMapping(
            path = "/{documentId}/versions/{versionNumber}/publish",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    Mono<ResponseEntity<PublishedDocumentResponse>> publish(
            Authentication authentication,
            @PathVariable
            @Size(max = 160)
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*") String documentId,
            @PathVariable int versionNumber,
            @Valid @RequestBody PublishDocumentRequest request
    ) {
        var scope = accessScopeProvider.currentScope(authentication);
        return Mono.fromCallable(() -> publicationService.publish(new PublishDocumentCommand(
                        scope.tenantId(), new DocumentId(documentId), new ActorId(scope.subjectId()), versionNumber,
                        request.expectedRevision(), request.effectiveFrom(), request.effectiveUntil(),
                        request.acl().stream().map(DocumentAclGrantRequest::toApplication).toList()
                )))
                .subscribeOn(Schedulers.boundedElastic())
                .map(PublishedDocumentResponse::from)
                .map(response -> ResponseEntity.accepted().body(response));
    }
}
