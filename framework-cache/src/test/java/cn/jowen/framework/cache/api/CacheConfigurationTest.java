package cn.jowen.framework.cache.api;

import cn.jowen.framework.cache.eviction.LruEvictionPolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CacheConfiguration} 测试。
 */
class CacheConfigurationTest {

    @Test
    void builder_defaultValues() {
        CacheConfiguration config = CacheConfiguration.builder().build();
        assertThat(config.maxSize()).isEqualTo(0L);
        assertThat(config.expireAfterWrite()).isNull();
        assertThat(config.expireAfterAccess()).isNull();
        assertThat(config.serializer()).isNull();
        assertThat(config.evictionPolicy()).isNull();
        assertThat(config.isNullValueCache()).isFalse();
        assertThat(config.jitter()).isNull();
        assertThat(config.isMutexLock()).isFalse();
    }

    @Test
    void builder_maxSize() {
        CacheConfiguration config = CacheConfiguration.builder().maxSize(256).build();
        assertThat(config.maxSize()).isEqualTo(256L);
    }

    @Test
    void builder_expireAfterWrite() {
        Duration duration = Duration.ofMinutes(10);
        CacheConfiguration config = CacheConfiguration.builder().expireAfterWrite(duration).build();
        assertThat(config.expireAfterWrite()).isEqualTo(duration);
    }

    @Test
    void builder_expireAfterAccess() {
        Duration duration = Duration.ofSeconds(30);
        CacheConfiguration config = CacheConfiguration.builder().expireAfterAccess(duration).build();
        assertThat(config.expireAfterAccess()).isEqualTo(duration);
    }

    @Test
    void builder_nullValueCache() {
        CacheConfiguration config = CacheConfiguration.builder().nullValueCache(true).build();
        assertThat(config.isNullValueCache()).isTrue();
    }

    @Test
    void builder_mutexLock() {
        CacheConfiguration config = CacheConfiguration.builder().mutexLock(true).build();
        assertThat(config.isMutexLock()).isTrue();
    }

    @Test
    void builder_jitter() {
        Duration jitter = Duration.ofSeconds(5);
        CacheConfiguration config = CacheConfiguration.builder().jitter(jitter).build();
        assertThat(config.jitter()).isEqualTo(jitter);
    }

    @Test
    void builder_evictionPolicy() {
        LruEvictionPolicy policy = new LruEvictionPolicy();
        CacheConfiguration config = CacheConfiguration.builder().evictionPolicy(policy).build();
        assertThat(config.evictionPolicy()).isSameAs(policy);
    }

    @Test
    void builder_allFields() {
        Duration writeTtl = Duration.ofMinutes(5);
        Duration accessTtl = Duration.ofSeconds(60);
        LruEvictionPolicy policy = new LruEvictionPolicy();
        Duration jitter = Duration.ofSeconds(2);

        CacheConfiguration config = CacheConfiguration.builder()
                .maxSize(100)
                .expireAfterWrite(writeTtl)
                .expireAfterAccess(accessTtl)
                .evictionPolicy(policy)
                .nullValueCache(true)
                .jitter(jitter)
                .mutexLock(true)
                .build();

        assertThat(config.maxSize()).isEqualTo(100L);
        assertThat(config.expireAfterWrite()).isEqualTo(writeTtl);
        assertThat(config.expireAfterAccess()).isEqualTo(accessTtl);
        assertThat(config.evictionPolicy()).isSameAs(policy);
        assertThat(config.isNullValueCache()).isTrue();
        assertThat(config.jitter()).isEqualTo(jitter);
        assertThat(config.isMutexLock()).isTrue();
    }

    @Test
    void builder_maxSize_zeroMeansUnbounded() {
        CacheConfiguration config = CacheConfiguration.builder().maxSize(0).build();
        assertThat(config.maxSize()).isEqualTo(0L);
    }

    @Test
    void builder_chainingReturnsSameBuilder() {
        CacheConfiguration.Builder builder = CacheConfiguration.builder();
        assertThat(builder.maxSize(10)).isSameAs(builder);
        assertThat(builder.expireAfterWrite(Duration.ofMinutes(1))).isSameAs(builder);
    }
}
