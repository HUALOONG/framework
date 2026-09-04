package cn.jowen.framework.cache.cache.multilevel;

import cn.jowen.framework.cache.api.Cache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultilevelCacheManagerSyncTest {

    @Mock Cache<Object, Object> local;
    @Mock Cache<Object, Object> remote;
    @Mock CacheSyncBroadcaster broadcaster;
    @Mock CacheSyncBroadcasterFactory factory;

    @Test
    void registerCacheWrapsWithSyncPublishingCacheAndPublishes() {
        when(factory.create(eq("c"), any())).thenReturn(broadcaster);

        MultilevelCacheManager manager = new MultilevelCacheManager();
        manager.setCacheSyncBroadcasterFactory(factory);
        manager.registerCache("c", local, remote, false, null, false);

        manager.getCache("c").put("k", "v");

        verify(broadcaster).publishPut("k", "v");
        verify(local).put("k", "v");
        verify(remote).put("k", "v");
    }

    @Test
    void withoutFactory_noWrapAndNoPublish() {
        MultilevelCacheManager manager = new MultilevelCacheManager();
        manager.registerCache("c", local, remote, false, null, false);
        manager.getCache("c").put("k", "v");
        verify(local).put("k", "v");
        verify(remote).put("k", "v");
    }
}
