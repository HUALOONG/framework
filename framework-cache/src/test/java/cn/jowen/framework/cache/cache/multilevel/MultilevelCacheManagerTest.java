package cn.jowen.framework.cache.cache.multilevel;

import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheStats;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link MultilevelCacheManager} 测试。
 */
@SuppressWarnings("unchecked")
class MultilevelCacheManagerTest {

    private final Cache<Object, Object> local = mock(Cache.class);
    private final Cache<Object, Object> remote = mock(Cache.class);
    private final MultilevelCacheManager manager = new MultilevelCacheManager();

    @Test
    void registerCache_defaultConfig_thenGetCache() {
        manager.registerCache("users", local, remote);
        Cache<String, String> cache = manager.getCache("users");
        assertThat(cache).isInstanceOf(MultilevelCache.class);
        assertThat(cache.name()).isEqualTo("users");
    }

    @Test
    void registerCache_fullConfig_thenGetCache() {
        manager.registerCache("orders", local, remote, true, Duration.ofMinutes(5), true);
        assertThat(manager.getCache("orders")).isNotNull();
    }

    @Test
    void getCache_unknownName_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> manager.getCache("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cache not found");
    }

    @Test
    void getStats_unknownCache_returnsNull() {
        assertThat(manager.getStats("missing")).isNull();
    }

    @Test
    void getStats_registeredCache_returnsStats() {
        when(local.stats()).thenReturn(mock(CacheStats.class));
        manager.registerCache("users", local, remote);
        assertThat(manager.getStats("users")).isNotNull();
    }

    @Test
    void cacheNames_containsRegisteredCaches() {
        manager.registerCache("a", local, remote);
        manager.registerCache("b", local, remote);
        assertThat(manager.cacheNames()).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void register_storesExternalCache() {
        Cache<Object, Object> external = mock(Cache.class);
        when(external.name()).thenReturn("external");
        manager.register(external);
        assertThat(manager.getCache("external")).isSameAs(external);
    }
}
