package com.xiaoding.javaai.knowledge.document.infrastructure;

import com.xiaoding.javaai.knowledge.document.domain.ObjectKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileDocumentObjectStoreTest {

    @TempDir
    Path root;

    @Test
    void writes_the_object_below_the_configured_root() throws Exception {
        LocalFileDocumentObjectStore store = new LocalFileDocumentObjectStore(root);
        ObjectKey key = new ObjectKey("tenant-a/knowledge/refund-policy/source.md");

        store.put(key, "text/markdown", "policy".getBytes(StandardCharsets.UTF_8));

        assertThat(Files.readString(root.resolve(key.value()))).isEqualTo("policy");
        assertThat(store.get(key)).isEqualTo("policy".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void rejects_an_object_key_that_escapes_the_root() {
        LocalFileDocumentObjectStore store = new LocalFileDocumentObjectStore(root);

        assertThatThrownBy(() -> store.put(
                new ObjectKey("../outside.txt"),
                "text/plain",
                "secret".getBytes(StandardCharsets.UTF_8)
        )).isInstanceOf(InvalidObjectKeyException.class);
    }
}
