package cn.jowen.framework.cache.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link DefaultCacheManager} 测试。
 */
class DefaultCacheManagerTest {

    private final DefaultCacheManager manager = new DefaultCacheManager();

    @Test
    void getCache_createsCache() {
        Cache<String, String> cache = manager.getCache("users");
        assertThat(cache).isNotNull();
        assertThat(cache.name()).isEqualTo("users");
    }

    @Test
    void getCache_returnsSameInstanceForSameName() {
        Cache<String, String> first = manager.getCache("users");
        Cache<String, String> second = manager.getCache("users");
        assertThat(second).isSameAs(first);
    }

    @Test
    void getCache_differentNamesCreateDifferentCaches() {
        assertThat(manager.getCache("a")).isNotSameAs(manager.getCache("b"));
    }

    @Test
    void cacheNames_containsCreatedCaches() {
        manager.getCache("alpha");
        manager.getCache("beta");
        assertThat(manager.cacheNames()).containsExactlyInAnyOrder("alpha", "beta");
    }

    @Test
    void getStats_returnsNullForUnknownCache() {
        assertThat(manager.getStats("unknown")).isNull();
    }

    @Test
    void getStats_returnsStatsForExistingCache() {
        manager.getCache("users");
        assertThat(manager.getStats("users")).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void register_storesExternalCache() {
        Cache<String, String> external = mock(Cache.class);
        when(external.name()).thenReturn("external");
        manager.register(external);
        assertThat(manager.getCache("external")).isSameAs(external);
        assertThat(manager.asMap()).containsKey("external");
    }
}
