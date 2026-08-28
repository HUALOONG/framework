package cn.jowen.framework.boot.autoconfigure.cache;

import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.api.DefaultCacheManager;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CacheStatsReporter} 单元测试，验证命中/未命中/命中率 gauge 注册。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class CacheStatsReporterTest {

    @Test
    void bindToRegistersHitMissGauges() {
        CacheManager manager = new DefaultCacheManager();
        Cache<String, String> cache = manager.getCache("user");
        cache.put("k", "v");
        cache.get("k");
        cache.get("missing");

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new CacheStatsReporter(manager).bindTo(registry);

        assertThat(registry.get("framework.cache.hits").tag("cache", "user").gauge().value()).isEqualTo(1.0);
        assertThat(registry.get("framework.cache.misses").tag("cache", "user").gauge().value()).isEqualTo(1.0);
        assertThat(registry.get("framework.cache.hitRate").tag("cache", "user").gauge().value()).isEqualTo(0.5);
    }
}