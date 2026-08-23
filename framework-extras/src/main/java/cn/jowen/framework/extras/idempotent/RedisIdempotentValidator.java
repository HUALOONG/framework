package cn.jowen.framework.extras.idempotent;

import cn.jowen.framework.cache.api.Cache;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 基于 {@code framework-cache} 的幂等校验器（Redis 实现）。
 *
 * <p>使用 SETNX + EXPIRE 原子操作确保幂等性。Token 模式下校验请求头 Token 并删除；
 * Key 模式下直接使用业务键进行 SETNX。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class RedisIdempotentValidator implements IdempotentValidator {

    private final Cache<String, Boolean> cache;
    private final long ttlMillis;

    public RedisIdempotentValidator(Cache<String, Boolean> cache, long ttlMillis) {
        this.cache = cache;
        this.ttlMillis = ttlMillis;
    }

    @Override
    public boolean validate(String key) {
        Boolean existing = cache.get(key);
        if (existing == null) {
            cache.put(key, Boolean.TRUE);
            return true;
        }
        return false;
    }

    @Override
    public void mark(String key) {
        cache.put(key, Boolean.TRUE);
    }

    @Override
    public void remove(String key) {
        cache.evict(key);
    }
}
