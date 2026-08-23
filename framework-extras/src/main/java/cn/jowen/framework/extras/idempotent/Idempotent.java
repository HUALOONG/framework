package cn.jowen.framework.extras.idempotent;

import cn.jowen.framework.cache.api.Cache;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.TimeUnit;

/**
 * 幂等控制器。基于 {@link Cache} 记录已处理的请求令牌，避免重复执行。
 *
 * <p>典型场景：表单重复提交、消息重复消费。令牌在 {@code ttl} 内有效，过期后允许再次执行。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public final class Idempotent {

    private final Cache<String, Boolean> store;
    private final long ttlMillis;

    /**
     * 构造幂等控制器。
     *
     * @param store     令牌存储（建议本地 TTL 缓存或分布式缓存）
     * @param ttlMillis 令牌存活毫秒（>0）
     */
    public Idempotent(Cache<String, Boolean> store, long ttlMillis) {
        this.store = store;
        this.ttlMillis = ttlMillis;
    }

    /**
     * 便捷单位构造。
     */
    public static Idempotent of(Cache<String, Boolean> store, long ttl, TimeUnit unit) {
        return new Idempotent(store, unit.toMillis(ttl));
    }

    /**
     * 尝试占用令牌。
     *
     * @param key 业务唯一键（如 请求ID+用户ID），不可为 {@code null}
     * @return 首次占用返回 {@code true}（可继续执行）；重复返回 {@code false}
     */
    public boolean tryOccupy(String key) {
        return store.putIfAbsent(key, Boolean.TRUE);
    }

    /**
     * @param key 释放已占用的令牌，允许后续重试
     */
    public void release(String key) {
        store.evict(key);
    }

    /**
     * @return 令牌 TTL（毫秒）
     */
    public long ttlMillis() {
        return ttlMillis;
    }
}
