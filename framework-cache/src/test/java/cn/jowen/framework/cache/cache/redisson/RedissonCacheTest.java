package cn.jowen.framework.cache.cache.redisson;

import cn.jowen.framework.cache.api.CacheConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedissonCacheTest {

    @Mock
    RedissonClient client;
    @Mock
    RMapCache<String, String> map;

    private RedissonCache<String, String> cacheWithTtl(long ttlMillis) {
        doReturn(map).when(client).getMapCache("c");
        CacheConfiguration config = CacheConfiguration.builder()
                .expireAfterWrite(Duration.ofMillis(ttlMillis))
                .build();
        return new RedissonCache<>(client, "c", config);
    }

    private RedissonCache<String, String> cacheNoTtl() {
        doReturn(map).when(client).getMapCache("c");
        CacheConfiguration config = CacheConfiguration.builder().build();
        return new RedissonCache<>(client, "c", config);
    }

    @Test
    void get_recordsHitAndMiss() {
        when(map.get("k1")).thenReturn("v1");
        when(map.get("k2")).thenReturn(null);

        RedissonCache<String, String> cache = cacheNoTtl();
        assertThat(cache.get("k1")).isEqualTo("v1");
        assertThat(cache.get("k2")).isNull();
        assertThat(cache.stats().hits()).isEqualTo(1);
        assertThat(cache.stats().misses()).isEqualTo(1);
    }

    @Test
    void getOptional_wrapsValue() {
        when(map.get("k")).thenReturn("v");
        RedissonCache<String, String> cache = cacheNoTtl();
        assertThat(cache.getOptional("k")).contains("v");
        assertThat(cache.getOptional("missing")).isEmpty();
    }

    @Test
    void put_withDefaultTtl_usesFastPut() {
        RedissonCache<String, String> cache = cacheWithTtl(1000);
        cache.put("k", "v");
        verify(map).fastPut(eq("k"), eq("v"), eq(1000L), any());
    }

    @Test
    void put_withExplicitTtl_usesFastPut() {
        RedissonCache<String, String> cache = cacheNoTtl();
        cache.put("k", "v", Duration.ofMillis(500));
        verify(map).fastPut(eq("k"), eq("v"), eq(500L), any());
    }

    @Test
    void put_withoutTtl_usesPut() {
        RedissonCache<String, String> cache = cacheNoTtl();
        cache.put("k", "v");
        verify(map).put(eq("k"), eq("v"));
        verify(map, never()).fastPut(any(), any(), anyLong(), any());
    }

    @Test
    void putIfAbsent_existing_returnsFalse() {
        when(map.get("k")).thenReturn("old");
        RedissonCache<String, String> cache = cacheNoTtl();
        assertThat(cache.putIfAbsent("k", "new")).isFalse();
        verify(map, never()).put(any(), any());
    }

    @Test
    void putIfAbsent_absent_returnsTrueAndStores() {
        when(map.get("k")).thenReturn(null);
        RedissonCache<String, String> cache = cacheNoTtl();
        assertThat(cache.putIfAbsent("k", "new")).isTrue();
        verify(map).get("k");
        verify(map).put(eq("k"), eq("new"));
    }

    @Test
    void evict_and_clear_and_size() {
        RedissonCache<String, String> cache = cacheNoTtl();
        cache.evict("k");
        cache.clear();
        cache.size();
        verify(map).remove("k");
        verify(map).clear();
        verify(map).size();
    }

    @Test
    void name_and_stats_and_createFactory() {
        doReturn(map).when(client).getMapCache("named");
        RedissonCache<String, String> cache = RedissonCache.create(client, "named",
                CacheConfiguration.builder().build());
        assertThat(cache.name()).isEqualTo("named");
        assertThat(cache.stats()).isNotNull();
    }
}
