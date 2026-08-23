package cn.jowen.framework.cache.cache.caffeine;

import cn.jowen.framework.cache.api.CacheConfiguration;
import cn.jowen.framework.cache.api.CacheStats;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CaffeineCache} 测试。
 */
class CaffeineCacheTest {

    private final CaffeineCache<String, String> cache =
            CaffeineCache.create("test", CacheConfiguration.builder().build());

    @Test
    void name_returnsCacheName() {
        assertThat(cache.name()).isEqualTo("test");
    }

    @Test
    void get_nonExistentKey_returnsNull() {
        assertThat(cache.get("nonexistent")).isNull();
    }

    @Test
    void put_and_get_returnsValue() {
        cache.put("key1", "value1");
        assertThat(cache.get("key1")).isEqualTo("value1");
    }

    @Test
    void getOptional_hit_returnsOptionalWithValue() {
        cache.put("opt-key", "opt-value");
        assertThat(cache.getOptional("opt-key")).hasValue("opt-value");
    }

    @Test
    void getOptional_miss_returnsEmpty() {
        assertThat(cache.getOptional("missing")).isEmpty();
    }

    @Test
    void put_overwriteValue() {
        cache.put("k", "v1");
        cache.put("k", "v2");
        assertThat(cache.get("k")).isEqualTo("v2");
    }

    @Test
    void putIfAbsent_absentKey_insertsAndReturnsTrue() {
        boolean result = cache.putIfAbsent("absent", "new-value");
        assertThat(result).isTrue();
        assertThat(cache.get("absent")).isEqualTo("new-value");
    }

    @Test
    void putIfAbsent_existingKey_doesNotOverwriteAndReturnsFalse() {
        cache.put("existing", "original");
        boolean result = cache.putIfAbsent("existing", "other");
        assertThat(result).isFalse();
        assertThat(cache.get("existing")).isEqualTo("original");
    }

    @Test
    void evict_removesKey() {
        cache.put("to-evict", "value");
        cache.evict("to-evict");
        assertThat(cache.get("to-evict")).isNull();
    }

    @Test
    void clear_removesAllEntries() {
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        cache.clear();
        assertThat(cache.size()).isEqualTo(0);
    }

    @Test
    void size_increasesAfterPut() {
        long initialSize = cache.size();
        cache.put("size-test", "val");
        assertThat(cache.size()).isGreaterThan(initialSize);
    }

    @Test
    void stats_recordsHitsAndMisses() {
        cache.put("hit-key", "val");
        cache.get("hit-key"); // hit
        cache.get("miss-key"); // miss
        CacheStats stats = cache.stats();
        assertThat(stats.hits()).isGreaterThan(0);
        assertThat(stats.misses()).isGreaterThan(0);
    }

    @Test
    void create_withMaxSize() {
        CaffeineCache<String, String> c = CaffeineCache.create("bounded",
                CacheConfiguration.builder().maxSize(100).build());
        assertThat(c.name()).isEqualTo("bounded");
    }

    @Test
    void create_withExpireAfterWrite() {
        CaffeineCache<String, String> c = CaffeineCache.create("ttl",
                CacheConfiguration.builder().expireAfterWrite(Duration.ofMinutes(10)).build());
        c.put("ttl-key", "ttl-val");
        assertThat(c.get("ttl-key")).isEqualTo("ttl-val");
    }

    @Test
    void evict_nonExistentKey_noError() {
        cache.evict("nonexistent-key");
    }

    @Test
    void putIfAbsent_multipleKeys() {
        boolean r1 = cache.putIfAbsent("m1", "v1");
        boolean r2 = cache.putIfAbsent("m2", "v2");
        assertThat(r1).isTrue();
        assertThat(r2).isTrue();
        assertThat(cache.get("m1")).isEqualTo("v1");
        assertThat(cache.get("m2")).isEqualTo("v2");
    }
}
