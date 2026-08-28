package cn.jowen.framework.boot.autoconfigure.health;

import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.api.CacheStats;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link HealthAutoConfiguration} / {@link BootHealthIndicator} 测试。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class HealthAutoConfigurationTest {

    private static final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HealthAutoConfiguration.class));

    @Test
    void bootHealthIndicator_withCacheManager_reportsCacheCount() {
        runner.withUserConfiguration(CacheTestConfig.class)
                .run(ctx -> {
                    BootHealthIndicator indicator = ctx.getBean(BootHealthIndicator.class);
                    Health health = indicator.health();
                    assertThat(health.getStatus()).isEqualTo(Status.UP);
                    assertThat(health.getDetails().get("cache.caches")).isEqualTo(2);
                });
    }

    @Test
    void bootHealthIndicator_noCacheManager_reportsNoCache() {
        runner.run(ctx -> {
            BootHealthIndicator indicator = ctx.getBean(BootHealthIndicator.class);
            Health health = indicator.health();
            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).doesNotContainKey("cache.caches");
        });
    }

    @Test
    void shouldRegisterFrameworkHealthIndicator() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(BootHealthIndicator.class);
        });
    }

    /**
     * 最小化 CacheManager 测试 stub，用于注入到 ApplicationContext。
     */
    static class StubCacheManager implements CacheManager {
        private final Set<String> names;
        private final Map<String, Cache<?, ?>> cacheMap = new LinkedHashMap<>();

        StubCacheManager(Set<String> names) {
            this.names = names;
        }

        @Override
        public <K, V> Cache<K, V> getCache(String name) {
            return (Cache<K, V>) cacheMap.computeIfAbsent(name, n -> new StubCache(n));
        }

        @Override
        public CacheStats getStats(String name) {
            return null;
        }

        @Override
        public Iterable<String> cacheNames() {
            return names;
        }

        @Override
        public void register(Cache<?, ?> cache) {
            cacheMap.put(cache.name(), cache);
        }

        @Override
        public Map<String, Cache<?, ?>> asMap() {
            return Collections.unmodifiableMap(cacheMap);
        }
    }

    static class StubCache implements Cache<Object, Object> {
        private final String name;

        StubCache(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Object get(Object key) {
            return null;
        }

        @Override
        public boolean putIfAbsent(Object key, Object value) {
            return false;
        }

        @Override
        public void evict(Object key) {
        }

        @Override
        public void clear() {
        }

        @Override
        public long size() {
            return 0;
        }

        @Override
        public CacheStats stats() {
            return new CacheStats();
        }

        @Override
        public void put(Object key, Object value) {
        }
    }

    @Configuration
    static class CacheTestConfig {
        @Bean
        CacheManager cacheManager() {
            return new StubCacheManager(java.util.Set.of("userCache", "productCache"));
        }
    }
}