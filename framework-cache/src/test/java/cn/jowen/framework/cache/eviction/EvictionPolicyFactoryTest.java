package cn.jowen.framework.cache.eviction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link EvictionPolicyFactory} 测试。
 */
class EvictionPolicyFactoryTest {

    @Test
    void create_lru_returnsRegisteredPolicy() {
        assertThat(EvictionPolicyFactory.create("lru")).isNotNull();
    }

    @Test
    void create_unknownType_returnsNull() {
        assertThat(EvictionPolicyFactory.create("unknown")).isNull();
    }

    @Test
    void getDefault_returnsLruPolicy() {
        assertThat(EvictionPolicyFactory.getDefault()).isNotNull();
        assertThat(EvictionPolicyFactory.getDefault()).isInstanceOf(LruEvictionPolicy.class);
    }

    @Test
    void register_thenCreate_returnsCustomPolicy() {
        EvictionPolicy custom = EvictionPolicy.LRU;
        EvictionPolicyFactory.register("custom", custom);
        assertThat(EvictionPolicyFactory.create("custom")).isSameAs(custom);
    }
}
