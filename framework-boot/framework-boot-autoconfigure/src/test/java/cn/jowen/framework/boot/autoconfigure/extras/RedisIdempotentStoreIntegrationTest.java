package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.extras.web.idempotent.RedisIdempotentStore;
import cn.jowen.framework.extras.web.lock.RedisCommandExecutor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link RedisIdempotentStore} 集成测试：基于真实 Redis（Upstash）验证指纹语义。
 *
 * <p><b>触发条件</b>：必须设置系统属性 {@code -Dtest.redis.url}，否则本类所有用例跳过。
 * 这样 {@code mvn -o clean verify} 默认门禁不会执行任何 Redis 网络调用。
 *
 * <p>典型调用示例：
 * <pre>{@code
 * mvn -o -pl :framework-boot-autoconfigure clean verify \
 *   -Dtest=RedisIdempotentStoreIntegrationTest \
 *   -Dgroups=integration \
 *   -Dtest.redis.url="rediss://default:PASSWORD@host:port"
 * }</pre>
 *
 * <p>密码通过 URL 嵌入，不在源码中硬编码。
 *
 * @author 王飞
 * @since 0.0.3
 * @version 0.0.3
 */
@NullMarked
@Tag("integration")
class RedisIdempotentStoreIntegrationTest {

    @Nullable
    private static RedissonClient client;

    private static final String PREFIX = "jowen:test:int:idempotent:";

    @BeforeAll
    static void setUp() {
        String url = System.getProperty("test.redis.url");
        if (url == null || url.isBlank()) {
            return;
        }
        Config config = new Config();
        config.useSingleServer()
                .setAddress(url)
                .setTimeout(30_000)
                .setConnectTimeout(10_000);
        config.setCodec(new StringCodec());
        client = Redisson.create(config);
    }

    @AfterAll
    static void tearDown() {
        if (client == null) {
            return;
        }
        try {
            client.getKeys().deleteByPattern(PREFIX + "*");
        } catch (Exception ignored) {
        }
        client.shutdown();
    }

    private RedissonCommandExecutor executor() {
        assertThat(client).isNotNull();
        return new RedissonCommandExecutor(client);
    }

    private String freshKey(String suffix) {
        return PREFIX + System.identityHashCode(this) + ":" + suffix;
    }

    private void assumeRedisAvailable() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                client != null,
                "test.redis.url 未设置，集成测试跳过");
    }

    private void cleanup(String... keys) {
        if (client == null) {
            return;
        }
        for (String k : keys) {
            try {
                client.getBucket(k).delete();
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    void tryMark_returnsTrueOnFirstCallThenFalse() {
        assumeRedisAvailable();
        String key = freshKey("first-then-dup");
        RedisIdempotentStore store = new RedisIdempotentStore(executor());

        assertThat(store.tryMark(key, 60, TimeUnit.SECONDS)).isTrue();
        assertThat(store.tryMark(key, 60, TimeUnit.SECONDS)).isFalse();

        cleanup(key);
    }

    @Test
    void distinctKeysAreIndependent() {
        assumeRedisAvailable();
        String keyA = freshKey("key-a");
        String keyB = freshKey("key-b");
        RedisIdempotentStore store = new RedisIdempotentStore(executor());

        assertThat(store.tryMark(keyA, 60, TimeUnit.SECONDS)).isTrue();
        assertThat(store.tryMark(keyA, 60, TimeUnit.SECONDS)).isFalse();
        assertThat(store.tryMark(keyB, 60, TimeUnit.SECONDS)).isTrue();

        cleanup(keyA, keyB);
    }

    @Test
    void tryMark_prefixIsHonored() {
        assumeRedisAvailable();
        String key = freshKey("prefixed");
        RedisIdempotentStore prefixed = new RedisIdempotentStore(executor(), "my-app:idem:");
        RedisIdempotentStore plain = new RedisIdempotentStore(executor());

        assertThat(prefixed.tryMark(key, 60, TimeUnit.SECONDS)).isTrue();
        assertThat(plain.tryMark(key, 60, TimeUnit.SECONDS)).isTrue();

        cleanup(key);
    }

    @Test
    void keyExpiresAfterTtl_thenTryMarkSucceedsAgain() throws InterruptedException {
        assumeRedisAvailable();
        String key = freshKey("expire");
        RedisIdempotentStore store = new RedisIdempotentStore(executor());

        assertThat(store.tryMark(key, 1, TimeUnit.SECONDS)).isTrue();
        assertThat(store.tryMark(key, 1, TimeUnit.SECONDS)).isFalse();

        Thread.sleep(1_500L);
        assertThat(store.tryMark(key, 1, TimeUnit.SECONDS)).isTrue();

        cleanup(key);
    }

    @Test
    void concurrentTryMarkOnlyOneSucceeds() throws InterruptedException {
        assumeRedisAvailable();
        String key = freshKey("concurrent");
        RedisIdempotentStore store = new RedisIdempotentStore(executor());

        AtomicInteger success = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            final int idx = i;
            new Thread(() -> {
                try {
                    start.await();
                    if (store.tryMark(key, 60, TimeUnit.SECONDS)) {
                        success.incrementAndGet();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        done.await(10, TimeUnit.SECONDS);

        // CAS 语义：10 个并发只有 1 个成功
        assertThat(success.get()).isEqualTo(1);

        cleanup(key);
    }

    @Test
    void removeDeletesPrefixedKey() {
        assumeRedisAvailable();
        String key = freshKey("remove");
        RedisIdempotentStore store = new RedisIdempotentStore(executor(), "p:");

        store.tryMark(key, 60, TimeUnit.SECONDS);
        store.remove(key);
        assertThat(store.tryMark(key, 60, TimeUnit.SECONDS)).isTrue();

        cleanup(key);
    }

    @Test
    void tryMarkRejectsNullKey() {
        assumeRedisAvailable();
        RedisIdempotentStore store = new RedisIdempotentStore(executor());

        assertThatThrownBy(() -> store.tryMark(null, 60, TimeUnit.SECONDS))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("key must not be null");
    }

    @Test
    void tryMarkRejectsNonPositiveExpire() {
        assumeRedisAvailable();
        RedisIdempotentStore store = new RedisIdempotentStore(executor());

        assertThatThrownBy(() -> store.tryMark("k", 0, TimeUnit.SECONDS))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expire must be positive");
        assertThatThrownBy(() -> store.tryMark("k", -1, TimeUnit.SECONDS))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expire must be positive");
    }

    @Test
    void removeRejectsNullKey() {
        assumeRedisAvailable();
        RedisIdempotentStore store = new RedisIdempotentStore(executor());

        assertThatThrownBy(() -> store.remove(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("key must not be null");
    }

    @Test
    void constructorRejectsNullExecutor() {
        assumeRedisAvailable();
        assertThatThrownBy(() -> new RedisIdempotentStore(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("executor must not be null");
    }

    @Test
    void constructorRejectsBlankKeyPrefix() {
        assumeRedisAvailable();
        RedisCommandExecutor exec = executor();
        assertThatThrownBy(() -> new RedisIdempotentStore(executor(), "  "))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("keyPrefix must not be blank");
    }
}