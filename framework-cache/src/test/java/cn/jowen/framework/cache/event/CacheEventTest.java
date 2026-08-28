package cn.jowen.framework.cache.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 缓存事件 POJO 与事件类型枚举的单元测试。
 *
 * <p>覆盖：抽象基类 {@link CacheEvent} 的构造与访问器（经由各具体子类实例化）、
 * {@link CacheHitEvent} / {@link CacheMissEvent} / {@link CacheEvictEvent} /
 * {@link CachePutEvent}（含 ttlMillis 字段）的字段读取，以及 {@link CacheEventType} 枚举的
 * {@code values()} / {@code valueOf()} / {@code name()}。
 */
class CacheEventTest {

    private final Instant now = Instant.ofEpochMilli(1_700_000_000_000L);

    @Test
    void hitEvent_exposesFields() {
        CacheHitEvent event = new CacheHitEvent("users", "k1", now);
        assertThat(event.cacheName()).isEqualTo("users");
        assertThat(event.key()).isEqualTo("k1");
        assertThat(event.timestamp()).isEqualTo(now);
        assertThat(event.type()).isEqualTo(CacheEventType.HIT);
    }

    @Test
    void missEvent_exposesFields() {
        CacheMissEvent event = new CacheMissEvent("users", "k2", now);
        assertThat(event.cacheName()).isEqualTo("users");
        assertThat(event.key()).isEqualTo("k2");
        assertThat(event.type()).isEqualTo(CacheEventType.MISS);
    }

    @Test
    void evictEvent_exposesFields() {
        CacheEvictEvent event = new CacheEvictEvent("users", "k3", now);
        assertThat(event.cacheName()).isEqualTo("users");
        assertThat(event.key()).isEqualTo("k3");
        assertThat(event.type()).isEqualTo(CacheEventType.EVICT);
    }

    @Test
    void putEvent_exposesFieldsAndTtl() {
        CachePutEvent event = new CachePutEvent("users", "k4", now, 5000L);
        assertThat(event.cacheName()).isEqualTo("users");
        assertThat(event.key()).isEqualTo("k4");
        assertThat(event.type()).isEqualTo(CacheEventType.PUT);
        assertThat(event.ttlMillis()).isEqualTo(5000L);
    }

    @Test
    void eventType_valuesAndValueOf() {
        CacheEventType[] values = CacheEventType.values();
        assertThat(values).containsExactly(
                CacheEventType.HIT,
                CacheEventType.MISS,
                CacheEventType.PUT,
                CacheEventType.EVICT,
                CacheEventType.CLEAR);
        assertThat(CacheEventType.valueOf("HIT")).isEqualTo(CacheEventType.HIT);
        assertThat(CacheEventType.valueOf("CLEAR")).isEqualTo(CacheEventType.CLEAR);
        assertThat(CacheEventType.HIT.name()).isEqualTo("HIT");
    }
}
