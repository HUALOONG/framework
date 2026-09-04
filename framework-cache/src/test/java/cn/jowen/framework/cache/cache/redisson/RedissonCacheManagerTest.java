package cn.jowen.framework.cache.cache.redisson;

import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedissonCacheManagerTest {

    @Mock
    RedissonClient client;
    @Mock
    RMapCache<String, String> map;

    @Test
    void getCache_cachesPerName() {
        doReturn(map).when(client).getMapCache("a");
        RedissonCacheManager manager = new RedissonCacheManager(client);
        Cache<?, ?> c1 = manager.getCache("a");
        Cache<?, ?> c2 = manager.getCache("a");
        assertThat(c1).isSameAs(c2);
        assertThat(c1.name()).isEqualTo("a");
        // computeIfAbsent 仅创建一次
        verify(client, never()).getMapCache("b");
    }

    @Test
    void create_registersNamedCache() {
        doReturn(map).when(client).getMapCache("x");
        RedissonCacheManager manager = new RedissonCacheManager(client);
        Cache<?, ?> c = manager.create("x", CacheConfiguration.builder().build());
        assertThat(c.name()).isEqualTo("x");
        assertThat(manager.cacheNames()).contains("x");
    }

    @Test
    void getStats_absentReturnsNull_presentReturnsStats() {
        doReturn(map).when(client).getMapCache("a");
        RedissonCacheManager manager = new RedissonCacheManager(client);
        assertThat(manager.getStats("a")).isNull();
        manager.getCache("a");
        assertThat(manager.getStats("a")).isNotNull();
    }

    @Test
    void register_and_asMap() {
        doReturn(map).when(client).getMapCache("r");
        RedissonCacheManager manager = new RedissonCacheManager(client);
        Cache<?, ?> c = manager.getCache("r");
        manager.register(c);
        assertThat(manager.asMap()).containsKey("r");
        assertThat(manager.cacheNames()).contains("r");
    }

    @Test
    void shutdown_delegatesToClient() {
        RedissonCacheManager manager = new RedissonCacheManager(client);
        manager.shutdown();
        verify(client).shutdown();
    }
}
