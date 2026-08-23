package cn.jowen.framework.extras.idempotent;

import org.jspecify.annotations.NullMarked;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 本地单机幂等校验器（兜底实现）。
 *
 * <p>使用 {@code ConcurrentHashMap} 存储标记，配合定时清理过期条目。
 * 适用于单机部署或缓存不可用时的降级场景。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class LocalIdempotentValidator implements IdempotentValidator {

    private final ConcurrentHashMap<String, Long> markMap = new ConcurrentHashMap<>();
    private final long ttlMillis;
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    public LocalIdempotentValidator(long ttlMillis) {
        this.ttlMillis = ttlMillis;
        cleaner.scheduleAtFixedRate(this::cleanExpired, ttlMillis, ttlMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean validate(String key) {
        Long existing = markMap.putIfAbsent(key, System.currentTimeMillis() + ttlMillis);
        return existing == null;
    }

    @Override
    public void mark(String key) {
        markMap.put(key, System.currentTimeMillis() + ttlMillis);
    }

    @Override
    public void remove(String key) {
        markMap.remove(key);
    }

    private void cleanExpired() {
        long now = System.currentTimeMillis();
        markMap.entrySet().removeIf(e -> e.getValue() <= now);
    }
}
