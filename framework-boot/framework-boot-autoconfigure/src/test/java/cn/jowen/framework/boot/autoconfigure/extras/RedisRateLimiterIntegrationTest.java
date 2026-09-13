package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.extras.web.lock.RedisCommandExecutor;
import cn.jowen.framework.extras.web.ratelimit.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
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
 * 基于真实 Redis（Upstash）的限流器集成测试。
 *
 * <p><b>触发条件</b>：必须设置系统属性 {@code -Dtest.redis.url}，否则本类所有用例跳过。
 * 这样 {@code mvn -o clean verify} 默认门禁不会执行任何 Redis 网络调用。
 *
 * <p>典型调用示例：
 * <pre>{@code
 * mvn -o -pl :framework-boot-autoconfigure clean verify \
 *   -Dtest=RedisRateLimiterIntegrationTest \
 *   -Dgroups=integration \
 *   -Dtest.redis.url="rediss://default:PASSWORD@host:port"
 * }</pre>
 *
 * <p>密码通过 URL 嵌入，不在源码中硬编码。
 *
 * <p><b>关于窗口对齐</b>：{@link RateLimitKeys#fixedWindow(String, long)} 的窗口序号由
 * {@code now / windowMillis} 计算，因此「连续两次 {@code tryAcquire}」若跨越窗口边界
 * 会落到不同 key 上、计数不复用。本测试刻意使用远大于网络往返的窗口（300 秒），
 * 保证同一用例内的多次 {@code tryAcquire} 落在同一窗口，从而验证限流语义而非时间边界。
 *
 * @author 王飞
 * @since 0.0.3
 * @version 0.0.3
 */
@NullMarked
@Tag("integration")
class RedisRateLimiterIntegrationTest {

    @Nullable
    private static RedissonClient client;

    private static final String PREFIX = "jowen:test:int:ratelimit:";

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
            // 若 pattern 扫描不可用（某些 Redis 变体），静默忽略
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
                // 键可能已过期，静默忽略
            }
        }
    }

    /** 重复 tryAcquire，直到返回 false 或超时；避免因窗口边界对齐导致的假失败。 */
    private void assertEventuallyRejected(RateLimiter limiter, String key, long timeoutMillis)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        boolean rejected = false;
        while (System.currentTimeMillis() < deadline) {
            if (!limiter.tryAcquire(key)) {
                rejected = true;
                break;
            }
            Thread.sleep(50);
        }
        assertThat(rejected).as("key " + key + " should eventually be rejected").isTrue();
    }

    @Nested
    class FixedWindow {

        @Test
        void allowsUpToPermitsThenRejects() throws InterruptedException {
            assumeRedisAvailable();
            String key = freshKey("fixed-window");
            // 300 秒窗口，保证多次 tryAcquire 落在同一窗口序号内
            RedisFixedWindowRateLimiter limiter =
                    new RedisFixedWindowRateLimiter(executor(), new RateLimitKeys(), 3, 300, true);

            assertThat(limiter.tryAcquire(key)).isTrue();
            assertThat(limiter.tryAcquire(key)).isTrue();
            assertThat(limiter.tryAcquire(key)).isTrue();
            assertEventuallyRejected(limiter, key, 5_000L);

            cleanup(key);
        }

        @Test
        void allowsAfterWindowExpires() throws InterruptedException {
            assumeRedisAvailable();
            String key = freshKey("fixed-window-expire");
            // 1 秒窗口：耗尽后等待窗口滑过
            RedisFixedWindowRateLimiter limiter =
                    new RedisFixedWindowRateLimiter(executor(), new RateLimitKeys(), 1, 1, true);

            assertThat(limiter.tryAcquire(key)).isTrue();
            assertEventuallyRejected(limiter, key, 2_000L);

            // 窗口过期后重新允许
            Thread.sleep(1_500L);
            assertThat(limiter.tryAcquire(key)).isTrue();

            cleanup(key);
        }

        @Test
        void distinctKeysAreIsolated() throws InterruptedException {
            assumeRedisAvailable();
            String keyA = freshKey("fixed-a");
            String keyB = freshKey("fixed-b");
            RedisFixedWindowRateLimiter limiter =
                    new RedisFixedWindowRateLimiter(executor(), new RateLimitKeys(), 1, 300, true);

            assertThat(limiter.tryAcquire(keyA)).isTrue();
            assertEventuallyRejected(limiter, keyA, 2_000L);
            // keyB 不受影响
            assertThat(limiter.tryAcquire(keyB)).isTrue();

            cleanup(keyA, keyB);
        }

        @Test
        void concurrentOnlyOneSucceedsWhenPermitsIsOne() throws InterruptedException {
            assumeRedisAvailable();
            String key = freshKey("fixed-concurrent");
            RedisFixedWindowRateLimiter limiter =
                    new RedisFixedWindowRateLimiter(executor(), new RateLimitKeys(), 1, 300, true);

            AtomicInteger success = new AtomicInteger();
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(10);

            for (int i = 0; i < 10; i++) {
                final int idx = i;
                new Thread(() -> {
                    try {
                        start.await();
                        if (limiter.tryAcquire(key)) {
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
    }

    @Nested
    class TokenBucket {

        @Test
        void allowsUpToCapacityThenRejects() throws InterruptedException {
            assumeRedisAvailable();
            String key = freshKey("token-bucket");
            RedisTokenBucketRateLimiter limiter =
                    new RedisTokenBucketRateLimiter(executor(), new RateLimitKeys(), 3, 1, true);

            assertThat(limiter.tryAcquire(key)).isTrue();
            assertThat(limiter.tryAcquire(key)).isTrue();
            assertThat(limiter.tryAcquire(key)).isTrue();
            assertEventuallyRejected(limiter, key, 5_000L);

            cleanup(key);
        }

        @Test
        void refillsAfterWaiting() throws InterruptedException {
            assumeRedisAvailable();
            String key = freshKey("token-bucket-refill");
            RedisTokenBucketRateLimiter limiter =
                    new RedisTokenBucketRateLimiter(executor(), new RateLimitKeys(), 5, 1, true);

            // 耗尽
            for (int i = 0; i < 5; i++) {
                assertThat(limiter.tryAcquire(key)).isTrue();
            }
            assertEventuallyRejected(limiter, key, 2_000L);

            // rate=1/s，等待一个 refill 周期
            Thread.sleep(1_200L);
            assertThat(limiter.tryAcquire(key)).isTrue();

            cleanup(key);
        }

        @Test
        void distinctDimensionsAreIsolated() throws InterruptedException {
            assumeRedisAvailable();
            String dimA = freshKey("tb-dim-a");
            String dimB = freshKey("tb-dim-b");
            RateLimitKeys keys = new RateLimitKeys();
            RedisTokenBucketRateLimiter limiterA =
                    new RedisTokenBucketRateLimiter(executor(), keys, 1, 1, true);
            RedisTokenBucketRateLimiter limiterB =
                    new RedisTokenBucketRateLimiter(executor(), keys, 10, 1, true);

            assertThat(limiterA.tryAcquire(dimA)).isTrue();
            assertEventuallyRejected(limiterA, dimA, 2_000L);
            // 不同维度互不影响
            assertThat(limiterB.tryAcquire(dimB)).isTrue();
            assertThat(limiterB.tryAcquire(dimB)).isTrue();

            cleanup(dimA, dimB);
        }
    }

    @Nested
    class SlidingWindow {

        @Test
        void allowsUpToPermitsThenRejects() throws InterruptedException {
            assumeRedisAvailable();
            String key = freshKey("sliding-window");
            RedisSlidingWindowRateLimiter limiter =
                    new RedisSlidingWindowRateLimiter(executor(), new RateLimitKeys(), 3, 300, true);

            assertThat(limiter.tryAcquire(key)).isTrue();
            assertThat(limiter.tryAcquire(key)).isTrue();
            assertThat(limiter.tryAcquire(key)).isTrue();
            assertEventuallyRejected(limiter, key, 5_000L);

            cleanup(key);
        }

        @Test
        void allowsAfterWindowSlides() throws InterruptedException {
            assumeRedisAvailable();
            String key = freshKey("sliding-window-slide");
            // 1 秒窗口
            RedisSlidingWindowRateLimiter limiter =
                    new RedisSlidingWindowRateLimiter(executor(), new RateLimitKeys(), 2, 1, true);

            assertThat(limiter.tryAcquire(key)).isTrue();
            assertThat(limiter.tryAcquire(key)).isTrue();
            assertEventuallyRejected(limiter, key, 2_000L);

            // 窗口滑出后旧记录被清理，重新允许
            Thread.sleep(1_500L);
            assertThat(limiter.tryAcquire(key)).isTrue();

            cleanup(key);
        }

        @Test
        void concurrentRequestsRespectPermits() throws InterruptedException {
            assumeRedisAvailable();
            String key = freshKey("sliding-concurrent");
            RedisSlidingWindowRateLimiter limiter =
                    new RedisSlidingWindowRateLimiter(executor(), new RateLimitKeys(), 3, 300, true);

            AtomicInteger success = new AtomicInteger();
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(10);

            for (int i = 0; i < 10; i++) {
                final int idx = i;
                new Thread(() -> {
                    try {
                        start.await();
                        if (limiter.tryAcquire(key)) {
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

            // 并发 10 个请求，由 Lua 脚本原子保证只有 3 个成功
            assertThat(success.get()).isEqualTo(3);

            cleanup(key);
        }
    }

    @Nested
    class LeakyBucket {

        @Test
        void allowsWhileBucketNotFullThenRejects() throws InterruptedException {
            assumeRedisAvailable();
            String key = freshKey("leaky-bucket");
            // 容量 10，每秒漏 1 个
            RedisLeakyBucketRateLimiter limiter =
                    new RedisLeakyBucketRateLimiter(executor(), new RateLimitKeys(), 10, 1, true);

            for (int i = 0; i < 10; i++) {
                assertThat(limiter.tryAcquire(key)).isTrue();
            }
            assertEventuallyRejected(limiter, key, 5_000L);

            cleanup(key);
        }

        @Test
        void waterLevelDropsAfterWaiting() throws InterruptedException {
            assumeRedisAvailable();
            String key = freshKey("leaky-bucket-level");
            RedisLeakyBucketRateLimiter limiter =
                    new RedisLeakyBucketRateLimiter(executor(), new RateLimitKeys(), 5, 1, true);

            // 装满
            for (int i = 0; i < 5; i++) {
                assertThat(limiter.tryAcquire(key)).isTrue();
            }
            assertEventuallyRejected(limiter, key, 2_000L);

            // rate=1/s，等待 2 秒应至少漏 2 个
            Thread.sleep(2_200L);
            assertThat(limiter.tryAcquire(key)).isTrue();
            assertThat(limiter.tryAcquire(key)).isTrue();

            cleanup(key);
        }

        @Test
        void distinctDimensionsAreIsolated() throws InterruptedException {
            assumeRedisAvailable();
            String dimA = freshKey("lb-dim-a");
            String dimB = freshKey("lb-dim-b");
            RateLimitKeys keys = new RateLimitKeys();
            RedisLeakyBucketRateLimiter limiterA =
                    new RedisLeakyBucketRateLimiter(executor(), keys, 1, 1, true);
            RedisLeakyBucketRateLimiter limiterB =
                    new RedisLeakyBucketRateLimiter(executor(), keys, 10, 1, true);

            assertThat(limiterA.tryAcquire(dimA)).isTrue();
            assertEventuallyRejected(limiterA, dimA, 2_000L);
            assertThat(limiterB.tryAcquire(dimB)).isTrue();
            assertThat(limiterB.tryAcquire(dimB)).isTrue();

            cleanup(dimA, dimB);
        }
    }

    @Test
    void rejectsExecutorThatDoesNotSupportScript() {
        assumeRedisAvailable();
        assertThatThrownBy(() -> new RedisFixedWindowRateLimiter(
                new NoScriptExecutor(), new RateLimitKeys(), 3, 300, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("must support script");
    }

    /** 不支持脚本的执行器（使用 default 方法），用于构造期校验验证。 */
    @NullMarked
    static final class NoScriptExecutor implements RedisCommandExecutor {
        @Override
        public boolean setIfAbsent(String key, String value, long expireMillis) {
            return true;
        }

        @Override
        public @Nullable String get(String key) {
            return null;
        }

        @Override
        public void delete(String key) {
        }

        @Override
        public boolean deleteIfMatch(String key, String value) {
            return false;
        }
    }
}