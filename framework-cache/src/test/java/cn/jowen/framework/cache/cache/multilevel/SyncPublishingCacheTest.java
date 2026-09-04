package cn.jowen.framework.cache.cache.multilevel;

import cn.jowen.framework.cache.api.Cache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

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
}
