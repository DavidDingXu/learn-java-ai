package com.xiaoding.javaai.knowledge.document.infrastructure;

import com.xiaoding.javaai.knowledge.document.application.port.DocumentObjectStore;
import com.xiaoding.javaai.knowledge.document.domain.ObjectKey;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class LocalFileDocumentObjectStore implements DocumentObjectStore {

    private final Path root;

    public LocalFileDocumentObjectStore(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public void put(ObjectKey key, String mediaType, byte[] content) {
        Path target = root.resolve(key.value()).normalize();
        if (!target.startsWith(root)) throw new InvalidObjectKeyException(key.value());

        try {
            Files.createDirectories(target.getParent());
            Path temporary = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".upload");
            try {
                Files.write(temporary, content);
                moveAtomicallyWhenSupported(temporary, target);
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException error) {
            throw new DocumentObjectStorageException("failed to store document object " + key.value(), error);
        }
    }

    private static void moveAtomicallyWhenSupported(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
