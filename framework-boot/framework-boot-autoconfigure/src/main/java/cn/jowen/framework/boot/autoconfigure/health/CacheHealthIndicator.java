package cn.jowen.framework.boot.autoconfigure.health;

import cn.jowen.framework.cache.api.CacheManager;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/**
 * 缓存健康指示器：上报框架缓存管理器当前的缓存数量。
 *
 * <p>基于框架 {@link CacheManager}（与 {@link BootHealthIndicator} 同源），
 * 遍历 {@code cacheNames()} 汇总缓存个数，供 actuator health 端点聚合。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class CacheHealthIndicator implements HealthIndicator {

    private final CacheManager cacheManager;

    /**
     * 构造健康指示器。
     *
     * @param cacheManager 缓存管理器，不可为 {@code null}
     */
    public CacheHealthIndicator(CacheManager cacheManager) {
        if (cacheManager == null) {
            throw new IllegalArgumentException("cacheManager must not be null");
        }
        this.cacheManager = cacheManager;
    }

    @Override
    public Health health() {
        int count = 0;
        for (String ignored : cacheManager.cacheNames()) {
            count++;
        }
        return Health.up().withDetail("caches", count).build();
    }
}