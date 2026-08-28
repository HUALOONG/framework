package cn.jowen.framework.cache.cache.multilevel;

import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheStats;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MultilevelCache} 测试。
 */
@SuppressWarnings("unchecked")
class MultilevelCacheTest {

    private final Cache<String, String> local = mock(Cache.class);
    private final Cache<String, String> remote = mock(Cache.class);

    private MultilevelCache<String, String> newCache() {
        return new MultilevelCache<>("multi", local, remote);
    }

    @Test
    void name_returnsConfiguredName() {
        assertThat(newCache().name()).isEqualTo("multi");
    }

    @Test
    void get_localHit_returnsLocalValue() {
        when(local.get("k")).thenReturn("v");
        assertThat(newCache().get("k")).isEqualTo("v");
        verify(remote, never()).get(any());
    }

    @Test
    void get_localMiss_remoteHit_backfillsLocal() {
        when(local.get("k")).thenReturn(null);
        when(remote.get("k")).thenReturn("rv");
        assertThat(newCache().get("k")).isEqualTo("rv");
        verify(local).put("k", "rv");
    }

    @Test
    void get_bothMiss_returnsNull() {
        when(local.get("k")).thenReturn(null);
        when(remote.get("k")).thenReturn(null);
        assertThat(newCache().get("k")).isNull();
        verify(local, never()).put(any(), any());
    }

    @Test
    void get_remoteMissWithNullValueCache_putsNullValue() {
        when(local.get("k")).thenReturn(null);
        when(remote.get("k")).thenReturn(null);
        MultilevelCache<String, String> cache =
                new MultilevelCache<>("multi", local, remote, true, Duration.ofMinutes(1), false);
        assertThat(cache.get("k")).isNull();
        verify(local).put(eq("k"), any());
    }

    @Test
    void get_mutexLock_serializesAndReturnsRemoteValue() {
        when(local.get("k")).thenReturn(null);
        when(remote.get("k")).thenReturn("rv");
        MultilevelCache<String, String> cache =
                new MultilevelCache<>("multi", local, remote, false, Duration.ZERO, true);
        assertThat(cache.get("k")).isEqualTo("rv");
        verify(local).put("k", "rv");
    }

    @Test
    void put_writesRemoteThenLocal() {
        newCache().put("k", "v");
        verify(remote).put("k", "v");
        verify(local).put("k", "v");
    }

    @Test
    void putWithTtl_writesBothWithTtl() {
        Duration ttl = Duration.ofSeconds(10);
        newCache().put("k", "v", ttl);
        verify(remote).put("k", "v", ttl);
        verify(local).put("k", "v", ttl);
    }

    @Test
    void putIfAbsent_existingLocalValue_returnsFalse() {
        when(local.get("k")).thenReturn("v");
        assertThat(newCache().putIfAbsent("k", "nv")).isFalse();
        verify(remote, never()).put(any(), any());
    }

    @Test
    void putIfAbsent_missingValue_putsAndReturnsTrue() {
        when(local.get("k")).thenReturn(null);
        assertThat(newCache().putIfAbsent("k", "v")).isTrue();
        verify(remote).put("k", "v");
        verify(local).put("k", "v");
    }

    @Test
    void evict_removesFromBothLevels() {
        newCache().evict("k");
        verify(remote).evict("k");
        verify(local).evict("k");
    }

    @Test
    void clear_clearsBothLevels() {
        newCache().clear();
        verify(remote).clear();
        verify(local).clear();
    }

    @Test
    void size_delegatesToLocal() {
        when(local.size()).thenReturn(3L);
        assertThat(newCache().size()).isEqualTo(3L);
    }

    @Test
    void stats_delegatesToLocal() {
        CacheStats stats = mock(CacheStats.class);
        when(local.stats()).thenReturn(stats);
        assertThat(newCache().stats()).isSameAs(stats);
    }

    @Test
    void handleCacheMiss_localHit_returnsLocalValue() {
        when(local.get("k")).thenReturn("v");
        assertThat(newCache().handleCacheMiss("k", key -> "loaded")).isEqualTo("v");
        verify(remote, never()).get(any());
    }

    @Test
    void handleCacheMiss_remoteMiss_loaderLoadsAndBackfills() {
        when(local.get("k")).thenReturn(null);
        assertThat(newCache().handleCacheMiss("k", key -> "loaded")).isEqualTo("loaded");
        verify(remote).put("k", "loaded");
        verify(local).put("k", "loaded");
    }

    @Test
    void handleCacheMiss_loaderReturnsNull_returnsNull() {
        when(local.get("k")).thenReturn(null);
        assertThat(newCache().handleCacheMiss("k", key -> null)).isNull();
        verify(local, never()).put(any(), any());
    }
}
