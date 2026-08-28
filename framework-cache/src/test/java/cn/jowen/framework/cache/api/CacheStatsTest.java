package cn.jowen.framework.cache.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CacheStats} 测试。
 */
class CacheStatsTest {

    @Test
    void initial_state_isZero() {
        CacheStats stats = new CacheStats();
        assertThat(stats.hits()).isZero();
        assertThat(stats.misses()).isZero();
        assertThat(stats.total()).isZero();
        assertThat(stats.hitRate()).isEqualTo(1.0);
        assertThat(stats.missRate()).isZero();
    }

    @Test
    void recordHit_incrementsHits() {
        CacheStats stats = new CacheStats();
        stats.recordHit();
        stats.recordHit();
        stats.recordHit();
        assertThat(stats.hits()).isEqualTo(3);
        assertThat(stats.total()).isEqualTo(3);
        assertThat(stats.hitRate()).isEqualTo(1.0);
        assertThat(stats.missRate()).isZero();
    }

    @Test
    void recordMiss_incrementsMisses() {
        CacheStats stats = new CacheStats();
        stats.recordMiss();
        assertThat(stats.misses()).isEqualTo(1);
        assertThat(stats.total()).isEqualTo(1);
        assertThat(stats.hitRate()).isZero();
        assertThat(stats.missRate()).isEqualTo(1.0);
    }

    @Test
    void hitRate_isHitsOverTotal() {
        CacheStats stats = new CacheStats();
        stats.recordHit();
        stats.recordHit();
        stats.recordHit();
        stats.recordMiss();
        assertThat(stats.hits()).isEqualTo(3);
        assertThat(stats.misses()).isEqualTo(1);
        assertThat(stats.total()).isEqualTo(4);
        assertThat(stats.hitRate()).isEqualTo(0.75);
        assertThat(stats.missRate()).isEqualTo(0.25);
    }

    @Test
    void toString_includesCounters() {
        CacheStats stats = new CacheStats();
        stats.recordHit();
        stats.recordMiss();
        assertThat(stats.toString()).contains("hits=1").contains("misses=1");
    }
}
