package cn.dingxu.javaai.rag.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class EmbeddingBatchService {

    private final EmbeddingProvider provider;
    private final int batchSize;
    private final Map<EmbeddingCacheKey, List<Double>> cache = new LinkedHashMap<>();

    public EmbeddingBatchService(EmbeddingProvider provider) {
        this(provider, 16);
    }

    public EmbeddingBatchService(EmbeddingProvider provider, int batchSize) {
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
        this.batchSize = Math.max(1, batchSize);
    }

    public List<EmbeddingResult> embed(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        Map<EmbeddingCacheKey, DocumentChunk> missing = new LinkedHashMap<>();
        for (DocumentChunk chunk : chunks) {
            EmbeddingCacheKey key = cacheKey(chunk);
            if (!cache.containsKey(key)) {
                missing.putIfAbsent(key, chunk);
            }
        }
        embedMissing(missing);

        List<EmbeddingResult> results = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            EmbeddingCacheKey key = cacheKey(chunk);
            results.add(new EmbeddingResult(
                    chunk.documentId(),
                    chunk.chunkId(),
                    chunk.version(),
                    cache.get(key),
                    !missing.containsKey(key)
            ));
        }
        return List.copyOf(results);
    }

    private void embedMissing(Map<EmbeddingCacheKey, DocumentChunk> missing) {
        List<Map.Entry<EmbeddingCacheKey, DocumentChunk>> entries = new ArrayList<>(missing.entrySet());
        for (int start = 0; start < entries.size(); start += batchSize) {
            int end = Math.min(start + batchSize, entries.size());
            List<Map.Entry<EmbeddingCacheKey, DocumentChunk>> batch = entries.subList(start, end);
            List<String> texts = batch.stream()
                    .map(entry -> entry.getValue().content())
                    .toList();
            List<List<Double>> vectors = provider.embed(texts);
            if (vectors.size() != batch.size()) {
                throw new IllegalStateException("embedding provider returned " + vectors.size()
                        + " vectors for " + batch.size() + " texts");
            }
            for (int i = 0; i < batch.size(); i++) {
                cache.put(batch.get(i).getKey(), List.copyOf(vectors.get(i)));
            }
        }
    }

    private EmbeddingCacheKey cacheKey(DocumentChunk chunk) {
        return new EmbeddingCacheKey(
                chunk.documentId(),
                chunk.chunkId(),
                chunk.version(),
                sha256(chunk.content())
        );
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private record EmbeddingCacheKey(
            String documentId,
            String chunkId,
            String version,
            String contentHash
    ) {
    }
}
