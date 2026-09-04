package cn.jowen.framework.cache.cache.caffeine;

import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheConfiguration;
import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.api.CacheStats;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CaffeineCacheManager} 测试。
 */
class CaffeineCacheManagerTest {

    @Test
    void defaultConstructor_createsManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        assertThat(manager).isNotNull();
    }

    @Test
    void getCache_createsCacheByName() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        Cache<String, String> cache = manager.getCache("myCache");
        assertThat(cache).isNotNull();
        assertThat(cache.name()).isEqualTo("myCache");
    }

    @Test
    void getCache_returnsSameInstanceOnSecondCall() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        Cache<String, String> first = manager.getCache("shared");
        Cache<String, String> second = manager.getCache("shared");
        assertThat(first).isSameAs(second);
    }

    @Test
    void getCache_differentNames_returnDifferentCaches() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        Cache<String, String> c1 = manager.getCache("cacheA");
        Cache<String, String> c2 = manager.getCache("cacheB");
        assertThat(c1).isNotSameAs(c2);
        assertThat(c1.name()).isEqualTo("cacheA");
        assertThat(c2.name()).isEqualTo("cacheB");
    }

    @Test
    void register_addsCacheToManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        CacheConfiguration config = CacheConfiguration.builder().build();
        CaffeineCache<String, String> custom = CaffeineCache.create("custom", config);
        manager.register(custom);
        assertThat(manager.cacheNames()).contains("custom");
    }

    @Test
    void cacheNames_returnsAllRegisteredNames() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.getCache("a");
        manager.getCache("b");
        assertThat(manager.cacheNames()).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void cacheNames_emptyBeforeUse() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        assertThat(manager.cacheNames()).isEmpty();
    }

    @Test
    void asMap_returnsInternalMap() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.getCache("x");
        Map<String, Cache<?, ?>> map = manager.asMap();
        assertThat(map).containsKey("x");
    }

    @Test
    void getStats_existingCache_returnsStats() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        Cache<String, String> cache = manager.getCache("statTest");
        cache.put("k", "v");
        cache.get("k");
        CacheStats stats = manager.getStats("statTest");
        assertThat(stats).isNotNull();
    }

    @Test
    void getStats_nonExistentCache_returnsNull() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        assertThat(manager.getStats("nonexistent")).isNull();
    }

    @Test
    void constructor_withConfig() {
        CacheConfiguration config = CacheConfiguration.builder()
                .maxSize(50)
                .expireAfterWrite(Duration.ofMinutes(5))
                .build();
        CaffeineCacheManager manager = new CaffeineCacheManager(config);
        Cache<String, String> cache = manager.getCache("configCache");
        assertThat(cache).isNotNull();
    }

    @Test
    void createDefault_returnsNewManager() {
        CacheManager manager = CaffeineCacheManager.createDefault();
        assertThat(manager).isNotNull();
        assertThat(manager.getCache("d1")).isNotNull();
    }

    @Test
    void create_withNameAndConfig_registersCache() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        CacheConfiguration config = CacheConfiguration.builder().maxSize(10).build();
        Cache<String, String> created = manager.create("explicit", config);
        assertThat(created).isNotNull();
        assertThat(created.name()).isEqualTo("explicit");
        assertThat(manager.cacheNames()).contains("explicit");
    }

    @Test
    void configure_withConfigurer_appliesOnCacheCreation() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        boolean[] invoked = {false};
        manager.configure(builder -> invoked[0] = true);
        Cache<String, String> cache = manager.getCache("configured");
        assertThat(cache).isNotNull();
        assertThat(invoked[0]).isTrue();
    }
}
