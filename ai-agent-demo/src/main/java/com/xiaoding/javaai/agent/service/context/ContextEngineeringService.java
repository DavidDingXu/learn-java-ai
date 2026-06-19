package com.xiaoding.javaai.agent.service.context;

import com.xiaoding.javaai.agent.service.memory.AgentContext;
import com.xiaoding.javaai.agent.service.memory.BusinessSnapshot;
import com.xiaoding.javaai.agent.service.memory.MemoryEntry;
import com.xiaoding.javaai.agent.service.memory.UserPreference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ContextEngineeringService {

    public EngineeredContext assemble(AgentContext context, ContextBudget budget) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (budget == null) {
            throw new IllegalArgumentException("budget must not be null");
        }

        List<ContextSlice> candidates = new ArrayList<>();
        context.businessSnapshot().ifPresent(snapshot -> candidates.add(businessSnapshotSlice(snapshot)));
        List<ContextSlice> conversationSlices = conversationSlices(context, budget);
        candidates.addAll(conversationSlices.stream()
                .filter(slice -> slice.type() == ContextSliceType.CONVERSATION)
                .toList());
        candidates.addAll(preferenceSlices(context.preferences()));
        candidates.addAll(conversationSlices.stream()
                .filter(slice -> slice.type() == ContextSliceType.CONVERSATION_SUMMARY)
                .toList());

        List<ContextSlice> accepted = new ArrayList<>();
        int used = 0;
        for (ContextSlice candidate : candidates) {
            if (used + candidate.estimatedTokens() <= budget.maxEstimatedTokens()) {
                accepted.add(candidate);
                used += candidate.estimatedTokens();
            }
        }

        return new EngineeredContext(accepted, used, orderedSources(accepted));
    }

    private ContextSlice businessSnapshotSlice(BusinessSnapshot snapshot) {
        String content = "业务快照: businessId=" + snapshot.businessId() + ", fields=" + snapshot.fields();
        return new ContextSlice(
                ContextSliceType.BUSINESS_SNAPSHOT,
                "businessSnapshot",
                content,
                estimateTokens(content),
                Map.of("businessId", snapshot.businessId(), "capturedAt", snapshot.capturedAt().toString())
        );
    }

    private List<ContextSlice> conversationSlices(AgentContext context, ContextBudget budget) {
        List<MemoryEntry> sorted = context.conversationWindow().stream()
                .sorted(Comparator.comparing(MemoryEntry::occurredAt))
                .toList();
        int keepCount = Math.min(budget.maxConversationMessages(), sorted.size());
        int keepFrom = Math.max(0, sorted.size() - keepCount);

        List<ContextSlice> slices = new ArrayList<>();
        List<MemoryEntry> overflow = sorted.subList(0, keepFrom);
        if (overflow.size() > 1) {
            String content = summarize(overflow);
            slices.add(new ContextSlice(
                    ContextSliceType.CONVERSATION_SUMMARY,
                    "conversation",
                    content,
                    estimateTokens(content),
                    Map.of("messageCount", overflow.size())
            ));
        }

        for (MemoryEntry entry : sorted.subList(keepFrom, sorted.size())) {
            String content = entry.content();
            slices.add(new ContextSlice(
                    ContextSliceType.CONVERSATION,
                    "conversation",
                    content,
                    estimateTokens(content),
                    Map.of("occurredAt", entry.occurredAt().toString(), "entryType", entry.type().name())
            ));
        }
        return slices;
    }

    private List<ContextSlice> preferenceSlices(List<UserPreference> preferences) {
        return preferences.stream()
                .sorted(Comparator.comparing(UserPreference::updatedAt))
                .map(preference -> {
                    String content = "用户偏好: " + preference.key() + "=" + preference.value();
                    return new ContextSlice(
                            ContextSliceType.PREFERENCE,
                            "preferences",
                            content,
                            estimateTokens(content),
                            Map.of("key", preference.key(), "scope", preference.scope().name())
                    );
                })
                .toList();
    }

    private String summarize(List<MemoryEntry> overflow) {
        return "历史摘要: " + overflow.size() + "条早期消息";
    }

    private int estimateTokens(String content) {
        if (content == null || content.isBlank()) {
            return 1;
        }
        int chineseChars = 0;
        int asciiChars = 0;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch < 128) {
                asciiChars++;
            } else {
                chineseChars++;
            }
        }
        return Math.max(1, chineseChars + (int) Math.ceil(asciiChars / 4.0));
    }

    private List<String> orderedSources(List<ContextSlice> slices) {
        Set<String> ordered = new LinkedHashSet<>();
        for (ContextSlice slice : slices) {
            ordered.add(slice.source());
        }
        return List.copyOf(ordered);
    }
}
