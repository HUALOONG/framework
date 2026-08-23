package cn.jowen.framework.extras.captcha.store;

import cn.jowen.framework.cache.api.CacheManager;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * 基于 {@code framework-cache} 的验证码答案存储。
 *
 * <p>验证码答案以 {@code captchaId} 为 key、标准答案为 value 存入缓存，TTL 由
 * {@link cn.jowen.framework.extras.captcha.CaptchaProperties#getTtlMillis()} 控制。
 * 缓存不可用时应降级为 {@link LocalCaptchaStore}。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class CacheCaptchaStore implements CaptchaStore {

    private static final String CACHE_NAME = "captcha";

    private final CacheManager cacheManager;

    public CacheCaptchaStore(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void put(String id, String answer, long ttlMillis) {
        cacheManager.getCache(CACHE_NAME).put(id, answer, Duration.ofMillis(ttlMillis));
    }

    @Override
    @Nullable
    public String get(String id) {
        return (String) cacheManager.getCache(CACHE_NAME).get(id);
    }

    @Override
    public void remove(String id) {
        cacheManager.getCache(CACHE_NAME).evict(id);
    }

    @Override
    public void clearExpired() {
        // 由 CacheManager 内部 TTL 机制自动清理，无需显式操作
    }
}
