package com.xiaoding.javaai.agent;

import com.xiaoding.javaai.agent.service.memory.BusinessSnapshot;
import com.xiaoding.javaai.agent.service.memory.ConversationMemoryStore;
import com.xiaoding.javaai.agent.service.memory.MemoryEntry;
import com.xiaoding.javaai.agent.service.memory.MemoryEntryType;
import com.xiaoding.javaai.agent.service.memory.MemoryScope;
import com.xiaoding.javaai.agent.service.memory.UserPreference;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationMemoryStoreTest {

    @Test
    void keepsOnlyRecentConversationMessagesWithinTenantAndSession() {
        ConversationMemoryStore store = new ConversationMemoryStore(3);

        store.append(new MemoryEntry("tenant-a", "session-1", "u1001", MemoryEntryType.USER_MESSAGE, "第一轮问题", Map.of(), Instant.parse("2026-06-07T10:00:00Z")));
        store.append(new MemoryEntry("tenant-a", "session-1", "u1001", MemoryEntryType.AGENT_MESSAGE, "第一轮建议", Map.of(), Instant.parse("2026-06-07T10:01:00Z")));
        store.append(new MemoryEntry("tenant-a", "session-1", "u1001", MemoryEntryType.USER_MESSAGE, "第二轮问题", Map.of(), Instant.parse("2026-06-07T10:02:00Z")));
        store.append(new MemoryEntry("tenant-a", "session-1", "u1001", MemoryEntryType.AGENT_MESSAGE, "第二轮建议", Map.of(), Instant.parse("2026-06-07T10:03:00Z")));
        store.append(new MemoryEntry("tenant-a", "session-2", "u1001", MemoryEntryType.USER_MESSAGE, "其他会话问题", Map.of(), Instant.parse("2026-06-07T10:04:00Z")));
        store.append(new MemoryEntry("tenant-b", "session-1", "u2001", MemoryEntryType.USER_MESSAGE, "其他租户问题", Map.of(), Instant.parse("2026-06-07T10:05:00Z")));

        List<MemoryEntry> window = store.recentConversation("tenant-a", "session-1");

        assertThat(window).extracting(MemoryEntry::content)
                .containsExactly("第一轮建议", "第二轮问题", "第二轮建议");
        assertThat(window).allSatisfy(entry -> {
            assertThat(entry.tenantId()).isEqualTo("tenant-a");
            assertThat(entry.sessionId()).isEqualTo("session-1");
        });
    }

    @Test
    void separatesBusinessSnapshotFromUserPreference() {
        ConversationMemoryStore store = new ConversationMemoryStore(5);

        store.saveBusinessSnapshot(new BusinessSnapshot(
                "tenant-a",
                "session-1",
                "T-1001",
                Map.of("ticketStatus", "OPEN", "orderStatus", "SHIPPED"),
                Instant.parse("2026-06-07T10:00:00Z")
        ));
        store.savePreference(new UserPreference(
                "tenant-a",
                "u1001",
                MemoryScope.LONG_TERM,
                "replyStyle",
                "回答尽量短一点",
                Instant.parse("2026-06-07T10:01:00Z")
        ));

        assertThat(store.businessSnapshot("tenant-a", "session-1", "T-1001"))
                .isPresent()
                .get()
                .satisfies(snapshot -> assertThat(snapshot.fields())
                        .containsEntry("ticketStatus", "OPEN")
                        .containsEntry("orderStatus", "SHIPPED"));

        assertThat(store.preferences("tenant-a", "u1001"))
                .extracting(UserPreference::key)
                .containsExactly("replyStyle");

        assertThat(store.businessSnapshot("tenant-a", "session-1", "replyStyle")).isEmpty();
    }

    @Test
    void doesNotExposePreferencesAcrossTenants() {
        ConversationMemoryStore store = new ConversationMemoryStore(5);
        store.savePreference(new UserPreference(
                "tenant-a",
                "u1001",
                MemoryScope.LONG_TERM,
                "language",
                "中文",
                Instant.parse("2026-06-07T10:00:00Z")
        ));

        assertThat(store.preferences("tenant-b", "u1001")).isEmpty();
    }
}
