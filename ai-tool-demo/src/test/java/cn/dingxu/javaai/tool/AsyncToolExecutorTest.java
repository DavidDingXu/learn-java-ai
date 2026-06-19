package cn.dingxu.javaai.tool;

import cn.dingxu.javaai.tool.service.AsyncToolExecutor;
import cn.dingxu.javaai.tool.service.ToolResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncToolExecutorTest {

    @Test
    void cachesReadOnlyToolResultByCacheKey() {
        AtomicInteger executions = new AtomicInteger();
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            AsyncToolExecutor executor = new AsyncToolExecutor(executorService, Duration.ofSeconds(1), 20);

            ToolResult first = executor.executeReadOnly("order.lookup", "tenant-a:O-1001", () -> {
                executions.incrementAndGet();
                return ToolResult.ok("订单查询成功", Map.of("orderId", "O-1001", "status", "SHIPPED"));
            });
            ToolResult second = executor.executeReadOnly("order.lookup", "tenant-a:O-1001", () -> {
                executions.incrementAndGet();
                return ToolResult.ok("不应该执行第二次", Map.of());
            });

            assertThat(first.data()).containsEntry("orderId", "O-1001");
            assertThat(second.data()).containsEntry("orderId", "O-1001");
            assertThat(executions).hasValue(1);
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void evictsLargeResultBeforeCaching() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            AsyncToolExecutor executor = new AsyncToolExecutor(executorService, Duration.ofSeconds(1), 2);

            ToolResult result = executor.executeReadOnly("ticket.search", "tenant-a:keyword", () -> ToolResult.ok("查询成功", Map.of(
                    "rows", java.util.List.of("T-1", "T-2", "T-3", "T-4"),
                    "total", 4
            )));

            assertThat(result.data()).containsEntry("total", 4);
            assertThat(result.data()).containsKey("evicted");
            List<?> rows = (List<?>) result.data().get("rows");
            assertThat(rows).isEqualTo(List.of("T-1", "T-2"));
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void returnsFailedResultWhenToolTimesOut() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            AsyncToolExecutor executor = new AsyncToolExecutor(executorService, Duration.ofMillis(30), 20);

            ToolResult result = executor.executeReadOnly("slow.tool", "tenant-a:slow", () -> {
                Thread.sleep(200);
                return ToolResult.ok("太晚了", Map.of());
            });

            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("timed out");
            assertThat(result.data()).containsEntry("toolName", "slow.tool");
        } finally {
            executorService.shutdownNow();
        }
    }
}
