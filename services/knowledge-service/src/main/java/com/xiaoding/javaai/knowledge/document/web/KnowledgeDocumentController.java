package com.xiaoding.javaai.knowledge.document.web;

import com.xiaoding.javaai.knowledge.document.application.DocumentPublicationService;
import com.xiaoding.javaai.knowledge.document.application.DocumentUploadService;
import com.xiaoding.javaai.knowledge.document.application.PublishDocumentCommand;
import com.xiaoding.javaai.knowledge.document.application.UploadDocumentCommand;
import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
@ConditionalOnProperty(name = "java-ai.knowledge.ingestion.enabled", havingValue = "true")
public final class KnowledgeDocumentController {

    private static final int MAX_BUFFERED_UPLOAD_BYTES = 5 * 1024 * 1024 + 1;

    private final DocumentUploadService uploadService;
    private final DocumentPublicationService publicationService;
    private final JwtDocumentWriteIdentityFactory identityFactory;
    private final Clock clock;

    public KnowledgeDocumentController(
            DocumentUploadService uploadService,
            DocumentPublicationService publicationService,
            JwtDocumentWriteIdentityFactory identityFactory,
            Clock clock
    ) {
        this.uploadService = uploadService;
        this.publicationService = publicationService;
        this.identityFactory = identityFactory;
        this.clock = clock;
    }

    @PostMapping(
            path = "/{documentId}/versions",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    Mono<ResponseEntity<UploadedDocumentResponse>> upload(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable
            @Size(max = 160)
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*") String documentId,
            @Valid @RequestPart("metadata") UploadDocumentMetadata metadata,
            @RequestPart("file") FilePart file
    ) {
        DocumentWriteIdentity identity = identityFactory.create(jwt);
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
                            new DocumentId(documentId), identity.tenantId(), metadata.title(), identity.actorId(),
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
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable
            @Size(max = 160)
            @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]*") String documentId,
            @PathVariable int versionNumber,
            @Valid @RequestBody PublishDocumentRequest request
    ) {
        DocumentWriteIdentity identity = identityFactory.create(jwt);
        return Mono.fromCallable(() -> publicationService.publish(new PublishDocumentCommand(
                        identity.tenantId(), new DocumentId(documentId), identity.actorId(), versionNumber,
                        request.expectedRevision(), request.effectiveFrom(), request.effectiveUntil(),
                        request.acl().stream().map(DocumentAclGrantRequest::toApplication).toList()
                )))
                .subscribeOn(Schedulers.boundedElastic())
                .map(PublishedDocumentResponse::from)
                .map(response -> ResponseEntity.accepted().body(response));
    }
}
