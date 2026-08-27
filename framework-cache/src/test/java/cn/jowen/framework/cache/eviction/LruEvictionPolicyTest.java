package cn.jowen.framework.cache.eviction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LruEvictionPolicyTest {

    private final LruEvictionPolicy policy = new LruEvictionPolicy();

    @Test
    void getType_returnsLru() {
        assertThat(policy.getType()).isEqualTo("lru");
    }

    @Test
    void shouldEvict_alwaysReturnsFalse() {
        assertThat(policy.shouldEvict(new TestEvictionContext())).isFalse();
    }

    @Test
    void getType_caseSensitive() {
        assertThat(policy.getType()).isEqualTo("lru");
        assertThat(policy.getType()).isNotEqualTo("LRU");
    }

    @Test
    void getType_returnsNonEmptyString() {
        assertThat(policy.getType()).isNotEmpty();
    }

    private static class TestEvictionContext implements EvictionContext {
        @Override
        public Object key() {
            return "test-key";
        }

        @Override
        public Object value() {
            return "test-value";
        }

        @Override
        public long accessCount() {
            return 5;
        }

        @Override
        public long writeTimeMillis() {
            return System.currentTimeMillis();
        }
    }
}
