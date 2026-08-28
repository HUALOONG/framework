package cn.jowen.framework.extras.idempotent;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link IdempotentTokenGenerator} 测试：雪花 ID 单调不重复、UUID 风格、参数校验。
 */
class IdempotentTokenGeneratorTest {

    @Test
    void snowflake_sequential_increasing() {
        IdempotentTokenGenerator generator = new IdempotentTokenGenerator(1L);
        long first = generator.nextId();
        long second = generator.nextId();
        long third = generator.nextId();
        assertThat(second).isGreaterThan(first);
        assertThat(third).isGreaterThan(second);
    }

    @Test
    void snowflake_noDuplicates_concurrently() throws InterruptedException {
        int threads = 8;
        int perThread = 2000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        Set<Long> ids = java.util.Collections.synchronizedSet(new HashSet<>());
        CountDownLatch latch = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            final int workId = t;
            pool.execute(() -> {
                try {
                    IdempotentTokenGenerator generator = new IdempotentTokenGenerator(workId);
                    for (int i = 0; i < perThread; i++) {
                        ids.add(generator.nextId());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();
        assertThat(ids).hasSize(threads * perThread);
    }

    @Test
    void snowflake_static_string_valid() {
        String id = IdempotentTokenGenerator.generateSnowflake();
        assertThat(id).isNotBlank();
        assertThat(Long.parseUnsignedLong(id)).isPositive();
        assertThat(IdempotentTokenGenerator.generateSnowflake(7L)).isNotBlank();
    }

    @Test
    void uuid_returns32Hex() {
        String uuid = IdempotentTokenGenerator.generateUuid();
        assertThat(uuid).hasSize(32).matches("[0-9a-f]{32}");
    }

    @Test
    void invalidWorkId_throws() {
        assertThatThrownBy(() -> new IdempotentTokenGenerator(-1L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IdempotentTokenGenerator(1024L)).isInstanceOf(IllegalArgumentException.class);
    }
}