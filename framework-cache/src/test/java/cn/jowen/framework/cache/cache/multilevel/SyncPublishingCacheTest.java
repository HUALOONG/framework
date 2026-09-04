package cn.jowen.framework.cache.cache.multilevel;

import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheStats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncPublishingCacheTest {

    @Mock Cache<Object, Object> delegate;
    @Mock CacheSyncBroadcaster broadcaster;

    @Test
    void putDelegatesAndPublishes() {
        SyncPublishingCache cache = new SyncPublishingCache(delegate, broadcaster);
        cache.put("k", "v");
        verify(delegate).put("k", "v");
        verify(broadcaster).publishPut("k", "v");
    }

    @Test
    void putWithTtlDelegatesAndPublishes() {
        SyncPublishingCache cache = new SyncPublishingCache(delegate, broadcaster);
        cache.put("k", "v", Duration.ofSeconds(10));
        verify(delegate).put(eq("k"), eq("v"), any(Duration.class));
        verify(broadcaster).publishPut("k", "v");
    }

    @Test
    void evictDelegatesAndPublishes() {
        SyncPublishingCache cache = new SyncPublishingCache(delegate, broadcaster);
        cache.evict("k");
        verify(delegate).evict("k");
        verify(broadcaster).publishEvict("k");
    }

    @Test
    void getDelegates() {
        SyncPublishingCache cache = new SyncPublishingCache(delegate, broadcaster);
        cache.get("k");
        verify(delegate).get("k");
    }

    @Test
    void nameDelegates() {
        when(delegate.name()).thenReturn("order-cache");
        SyncPublishingCache cache = new SyncPublishingCache(delegate, broadcaster);
        assertThat(cache.name()).isEqualTo("order-cache");
    }

    @Test
    void putIfAbsentDelegatesWithoutBroadcast() {
        // putIfAbsent 按设计不广播：仅 put/evict 触发跨节点同步，
        // 保持与「回环防护」语义一致，此处显式断言不产生广播交互
        when(delegate.putIfAbsent("k", "v")).thenReturn(true);
        SyncPublishingCache cache = new SyncPublishingCache(delegate, broadcaster);
        assertThat(cache.putIfAbsent("k", "v")).isTrue();
        verify(delegate).putIfAbsent("k", "v");
        verifyNoInteractions(broadcaster);
    }

    @Test
    void clearDelegatesWithoutBroadcast() {
        // clear 为全量失效，不走单键广播（远端节点应自行整表失效或依赖 TTL）
        SyncPublishingCache cache = new SyncPublishingCache(delegate, broadcaster);
        cache.clear();
        verify(delegate).clear();
        verifyNoInteractions(broadcaster);
    }

    @Test
    void sizeDelegates() {
        when(delegate.size()).thenReturn(42L);
        SyncPublishingCache cache = new SyncPublishingCache(delegate, broadcaster);
        assertThat(cache.size()).isEqualTo(42L);
    }

    @Test
    void statsDelegates() {
        CacheStats stats = new CacheStats();
        when(delegate.stats()).thenReturn(stats);
        SyncPublishingCache cache = new SyncPublishingCache(delegate, broadcaster);
        assertThat(cache.stats()).isSameAs(stats);
    }
}
