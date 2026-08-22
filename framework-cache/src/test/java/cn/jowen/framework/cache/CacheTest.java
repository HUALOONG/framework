package cn.jowen.framework.cache;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CacheTest {

    @Test
    void localCacheBasicCrud() {
        LocalCache<String, String> cache = CacheBuilder.<String, String>local("c1", 0L).build();
        assertThat(cache.get("k")).isNull();
        cache.put("k", "v");
        assertThat(cache.get("k")).isEqualTo("v");
        assertThat(cache.size()).isEqualTo(1);
        cache.evict("k");
        assertThat(cache.get("k")).isNull();
        assertThat(cache.size()).isZero();
    }

    @Test
    void statsTrackHitRate() {
        Cache<String, String> cache = CacheBuilder.<String, String>local("c2", 0L).build();
        cache.put("a", "1");
        cache.get("a"); // hit
        cache.get("b"); // miss
        cache.get("a"); // hit
        CacheStats stats = cache.stats();
        assertThat(stats.hits()).isEqualTo(2);
        assertThat(stats.misses()).isEqualTo(1);
        assertThat(stats.hitRate()).isCloseTo(2.0 / 3.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void ttlExpiresEntry() throws InterruptedException {
        LocalCache<String, String> cache = CacheBuilder.<String, String>local("c3", 50L).build();
        cache.put("k", "v");
        assertThat(cache.get("k")).isEqualTo("v");
        Thread.sleep(80);
        assertThat(cache.get("k")).isNull();
    }

    @Test
    void layeredCacheBackfillsLocal() {
        LocalCache<String, String> local = CacheBuilder.<String, String>local("local", 0L).build();
        LocalCache<String, String> backup = CacheBuilder.<String, String>local("backup", 0L).build();
        LayeredCache<String, String> layered =
                CacheBuilder.<String, String>layered("lv", local, backup).nullTtl(0L).build();

        // backup 有值，local 无 -> 回填 local
        backup.put("x", "remote");
        assertThat(layered.get("x")).isEqualTo("remote");
        assertThat(local.get("x")).isEqualTo("remote");

        // 写入同时落两层
        layered.put("y", "both");
        assertThat(backup.get("y")).isEqualTo("both");
    }

    @Test
    void layeredCacheGuardsPenetration() {
        LocalCache<String, String> local = CacheBuilder.<String, String>local("l", 0L).build();
        LocalCache<String, String> backup = CacheBuilder.<String, String>local("b", 0L).build();
        LayeredCache<String, String> layered =
                CacheBuilder.<String, String>layered("pen", local, backup).nullTtl(2_000L).build();

        AtomicInteger sourceCalls = new AtomicInteger();
        String v = layered.computeIfAbsent("missing", k -> {
            sourceCalls.incrementAndGet();
            return null; // 数据源也没有
        });
        assertThat(v).isNull();
        // 空值占位后再次访问不应回源
        layered.computeIfAbsent("missing", k -> {
            sourceCalls.incrementAndGet();
            return null;
        });
        assertThat(sourceCalls.get()).isLessThanOrEqualTo(1);
    }

    @Test
    void managerAggregatesStats() {
        DefaultCacheManager manager = new DefaultCacheManager();
        Cache<String, String> cache = manager.getCache("m1");
        cache.put("a", "1");
        cache.get("a");
        cache.get("z");
        CacheStats stats = manager.getStats("m1");
        assertThat(stats).isNotNull();
        assertThat(stats.hits()).isEqualTo(1);
        assertThat(stats.misses()).isEqualTo(1);
    }
}
