package cn.jowen.framework.cache.cache.multilevel;

import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheStats;
import cn.jowen.framework.cache.api.NullValue;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class MultilevelCacheTest {

    static final class MapCache<K, V> implements Cache<K, V> {
        @SuppressWarnings("unchecked")
        private final Map<Object, Object> store = new ConcurrentHashMap<>();
        private final String name;

        MapCache(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        @SuppressWarnings("unchecked")
        public V get(K key) {
            return (V) store.get(key);
        }

        @Override
        public void put(K key, V value) {
            store.put(key, value);
        }

        @Override
        public void put(K key, V value, Duration ttl) {
            store.put(key, value);
        }

        @Override
        public boolean putIfAbsent(K key, V value) {
            return store.putIfAbsent(key, value) == null;
        }

        @Override
        public void evict(K key) {
            store.remove(key);
        }

        @Override
        public void clear() {
            store.clear();
        }

        @Override
        public long size() {
            return store.size();
        }

        @Override
        public CacheStats stats() {
            return new CacheStats();
        }
    }

    @Test
    void put_get_missFillsLocal() {
        MapCache<String, String> local = new MapCache<>("local");
        MapCache<String, String> remote = new MapCache<>("remote");
        MultilevelCache<String, String> cache = new MultilevelCache<>("mc", local, remote);

        // 远程命中：回填本地
        remote.put("k", "v");
        assertThat(cache.get("k")).isEqualTo("v");
        assertThat(local.get("k")).isEqualTo("v");

        // 直接写入：双写
        cache.put("k2", "v2");
        assertThat(local.get("k2")).isEqualTo("v2");
        assertThat(remote.get("k2")).isEqualTo("v2");

        // 缺失返回 null
        assertThat(cache.get("missing")).isNull();

        // 基础方法
        assertThat(cache.name()).isEqualTo("mc");
        assertThat(cache.size()).isEqualTo(2);
        cache.evict("k");
        cache.clear();
        assertThat(cache.size()).isZero();
    }

    @Test
    void nullValueCache_cachesPlaceholder() {
        MapCache<String, String> local = new MapCache<>("local");
        MapCache<String, String> remote = new MapCache<>("remote");
        MultilevelCache<String, String> cache =
                new MultilevelCache<>("mc", local, remote, true, Duration.ZERO, false);

        // 远程未命中且开启空值缓存：写入占位并返回 null，占位不对外泄漏
        assertThat(cache.get("absent")).isNull();
        @SuppressWarnings("unchecked")
        MapCache rawLocal = local;
        assertThat(rawLocal.get("absent")).isInstanceOf(NullValue.class);
    }

    @Test
    void mutexLock_doubleChecks() {
        MapCache<String, String> local = new MapCache<>("local");
        MapCache<String, String> remote = new MapCache<>("remote");
        MultilevelCache<String, String> cache =
                new MultilevelCache<>("mc", local, remote, false, Duration.ZERO, true);

        remote.put("k", "v");
        assertThat(cache.get("k")).isEqualTo("v");
    }

    @Test
    void handleCacheMiss_loadsAndFills() {
        MapCache<String, String> local = new MapCache<>("local");
        MapCache<String, String> remote = new MapCache<>("remote");
        MultilevelCache<String, String> cache =
                new MultilevelCache<>("mc", local, remote, true, Duration.ZERO, false);

        String loaded = cache.handleCacheMiss("k", key -> "loaded-" + key);
        assertThat(loaded).isEqualTo("loaded-k");
        assertThat(remote.get("k")).isEqualTo("loaded-k");

        // 二次命中本地缓存，loader 不再执行
        String second = cache.handleCacheMiss("k", key -> "should-not-run");
        assertThat(second).isEqualTo("loaded-k");
    }

    @Test
    void mutexWithTtl_cachesNullPlaceholderWithTtl() {
        MapCache<String, String> local = new MapCache<>("local");
        MapCache<String, String> remote = new MapCache<>("remote");
        MultilevelCache<String, String> cache =
                new MultilevelCache<>("mc", local, remote, true, Duration.ofSeconds(5), true);

        // 互斥锁路径：double-check 后远程未命中且开启空值缓存（带 TTL）→ 写入占位并返回 null
        assertThat(cache.get("absent")).isNull();
        @SuppressWarnings("unchecked")
        MapCache rawLocal2 = local;
        assertThat(rawLocal2.get("absent")).isInstanceOf(NullValue.class);
    }

    @Test
    void putWithTtl_writesBothLevels() {
        MapCache<String, String> local = new MapCache<>("local");
        MapCache<String, String> remote = new MapCache<>("remote");
        MultilevelCache<String, String> cache = new MultilevelCache<>("mc", local, remote);

        // 带 TTL 写入必须同时落到远程与本地，两级不能只写一侧
        cache.put("k", "v", Duration.ofSeconds(30));
        assertThat(local.get("k")).isEqualTo("v");
        assertThat(remote.get("k")).isEqualTo("v");
    }

    @Test
    void putIfAbsent_absentReturnsTrue_existingShortCircuits() {
        MapCache<String, String> local = new MapCache<>("local");
        MapCache<String, String> remote = new MapCache<>("remote");
        MultilevelCache<String, String> cache = new MultilevelCache<>("mc", local, remote);

        // 本地不存在：写入成功返回 true，且双写两级
        assertThat(cache.putIfAbsent("k", "v")).isTrue();
        assertThat(local.get("k")).isEqualTo("v");
        assertThat(remote.get("k")).isEqualTo("v");

        // 本地已存在：以本地为判定基准短路返回 false，原值不得被覆盖
        assertThat(cache.putIfAbsent("k", "v2")).isFalse();
        assertThat(cache.get("k")).isEqualTo("v");
    }
}
