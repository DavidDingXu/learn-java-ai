package com.xiaoding.javaai.knowledge.document.web;

import com.xiaoding.javaai.knowledge.document.application.DocumentNotFoundException;
import com.xiaoding.javaai.knowledge.document.application.DocumentParsingException;
import com.xiaoding.javaai.knowledge.document.application.DocumentTooLargeException;
import com.xiaoding.javaai.knowledge.document.application.UnsupportedDocumentMediaTypeException;
import com.xiaoding.javaai.knowledge.document.domain.DocumentRevisionConflictException;
import com.xiaoding.javaai.knowledge.document.domain.DuplicateDocumentContentException;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = KnowledgeDocumentController.class)
public final class KnowledgeDocumentExceptionHandler {

    @ExceptionHandler(DocumentNotFoundException.class)
    ResponseEntity<ApiError> notFound(DocumentNotFoundException error) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("KNOWLEDGE_DOCUMENT_NOT_FOUND", error.getMessage()));
    }

    @ExceptionHandler({
            DocumentRevisionConflictException.class,
            DuplicateDocumentContentException.class,
            IllegalStateException.class
    })
    ResponseEntity<ApiError> conflict(RuntimeException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("KNOWLEDGE_DOCUMENT_CONFLICT", error.getMessage()));
    }

    @ExceptionHandler({DocumentTooLargeException.class, DataBufferLimitException.class})
    ResponseEntity<ApiError> tooLarge(RuntimeException error) {
        return ResponseEntity.status(413)
                .body(new ApiError("KNOWLEDGE_DOCUMENT_TOO_LARGE", error.getMessage()));
    }

    @ExceptionHandler({
            UnsupportedDocumentMediaTypeException.class,
            DocumentParsingException.class,
            IllegalArgumentException.class
    })
    ResponseEntity<ApiError> invalid(RuntimeException error) {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_KNOWLEDGE_DOCUMENT", error.getMessage()));
    }

    record ApiError(String code, String message) {
    }
}
