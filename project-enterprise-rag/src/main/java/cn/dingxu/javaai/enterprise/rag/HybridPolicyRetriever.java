package cn.dingxu.javaai.enterprise.rag;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class HybridPolicyRetriever {

    private final InMemoryChunkRepository chunkRepository;

    public HybridPolicyRetriever(InMemoryChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    public List<DocumentChunk> retrieve(String query, OperatorScope scope) {
        String normalizedQuery = normalize(query);
        return chunkRepository.findAll().stream()
                .filter(chunk -> chunk.accessibleBy(scope))
                .filter(chunk -> matches(normalizedQuery, chunk.content()))
                .sorted(Comparator
                        .comparingInt((DocumentChunk chunk) -> score(normalizedQuery, chunk.content())).reversed()
                        .thenComparing(DocumentChunk::documentId)
                        .thenComparing(DocumentChunk::ordinal))
                .limit(3)
                .toList();
    }

    private int score(String normalizedQuery, String content) {
        int score = 0;
        for (String token : tokens(normalizedQuery)) {
            if (content.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private boolean matches(String normalizedQuery, String content) {
        if (normalizedQuery.isBlank()) {
            return false;
        }
        for (String token : tokens(normalizedQuery)) {
            if (content.contains(token)) {
                return true;
            }
        }
        return content.contains(normalizedQuery);
    }

    private Set<String> tokens(String normalizedQuery) {
        return normalizedQuery.chars()
                .mapToObj(value -> String.valueOf((char) value))
                .filter(token -> !Set.of("怎", "么", "处", "理", "的", "了", "吗", "？", "?", "但", "和").contains(token))
                .collect(java.util.stream.Collectors.toSet());
    }

    private String normalize(String query) {
        return query == null ? "" : query.replaceAll("\\s+", "");
    }
}
