package cn.dingxu.javaai.tool.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AsyncToolExecutor {

    private final ExecutorService executorService;
    private final Duration timeout;
    private final int maxCollectionItems;
    private final Map<String, ToolResult> readOnlyCache = new ConcurrentHashMap<>();

    public AsyncToolExecutor() {
        this(Executors.newFixedThreadPool(8), Duration.ofSeconds(2), 50);
    }

    public AsyncToolExecutor(ExecutorService executorService, Duration timeout, int maxCollectionItems) {
        if (executorService == null) {
            throw new IllegalArgumentException("executorService must not be null");
        }
        this.executorService = executorService;
        this.timeout = timeout == null ? Duration.ofSeconds(2) : timeout;
        this.maxCollectionItems = Math.max(1, maxCollectionItems);
    }

    public ToolResult executeReadOnly(String toolName, String cacheKey, Callable<ToolResult> callable) {
        if (cacheKey != null && !cacheKey.isBlank()) {
            ToolResult cached = readOnlyCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }
        ToolResult result = execute(toolName, callable);
        ToolResult compacted = compactLargeResult(result);
        if (compacted.success() && cacheKey != null && !cacheKey.isBlank()) {
            readOnlyCache.put(cacheKey, compacted);
        }
        return compacted;
    }

    private ToolResult execute(String toolName, Callable<ToolResult> callable) {
        Future<ToolResult> future = executorService.submit(callable);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            return ToolResult.failed("tool " + toolName + " timed out after " + timeout.toMillis() + "ms", Map.of(
                    "toolName", toolName
            ));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ToolResult.failed("tool " + toolName + " interrupted", Map.of("toolName", toolName));
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            return ToolResult.failed("tool " + toolName + " failed: " + cause.getMessage(), Map.of("toolName", toolName));
        }
    }

    private ToolResult compactLargeResult(ToolResult result) {
        Map<String, Object> compacted = new LinkedHashMap<>();
        boolean evicted = false;
        for (Map.Entry<String, Object> entry : result.data().entrySet()) {
            Object value = entry.getValue();
            if (value instanceof List<?> list && list.size() > maxCollectionItems) {
                compacted.put(entry.getKey(), new ArrayList<>(list.subList(0, maxCollectionItems)));
                evicted = true;
            } else {
                compacted.put(entry.getKey(), value);
            }
        }
        if (evicted) {
            compacted.put("evicted", true);
            compacted.put("maxCollectionItems", maxCollectionItems);
        }
        if (result.success()) {
            return ToolResult.ok(result.message(), compacted);
        }
        return ToolResult.failed(result.message(), compacted);
    }
}
