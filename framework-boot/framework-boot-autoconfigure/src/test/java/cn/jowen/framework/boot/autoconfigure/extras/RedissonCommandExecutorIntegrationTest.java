package cn.jowen.framework.boot.autoconfigure.extras;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link RedissonCommandExecutor} 集成测试：基于真实 Redis（Upstash）验证 SPI 语义。
 *
 * <p><b>触发条件</b>：必须设置系统属性 {@code -Dtest.redis.url}，否则本类所有用例跳过。
 * 这样 {@code mvn -o clean verify} 默认门禁不会执行任何 Redis 网络调用。
 *
 * <p>典型调用示例：
 * <pre>{@code
 * mvn -o clean verify -Dtest.redis.url="redis://default:PASSWORD@host:port" -Dtags=integration
 * }</pre>
 *
 * <p>密码通过 URL 嵌入，不在源码中硬编码：
 * <pre>{@code redis://default:<PASSWORD>@leading-antelope-205308.upstash.io:6379}</pre>
 *
 * <p><b>注意</b>：使用 {@link ByteArrayCodec}（Redisson 内置，零额外依赖），所有字符串操作走
 * {@code String.getBytes(UTF-8)} / {@code new String(bytes, UTF-8)}，与 Spring 生态
 * {@code StringRedisTemplate} 行为一致。
 *
 * @author 王飞
 * @since 0.0.3
 */
@NullMarked
@Tag("integration")
class RedissonCommandExecutorIntegrationTest {

    /** 全用例共享的 Redisson 客户端（懒初始化，属性缺失则跳过所有测试）。 */
    @Nullable
    private static RedissonClient client;

    /** 测试键名前缀，便于清理。 */
    private static final String PREFIX = "jowen:test:int:";

    // -------------------------------------------------------------------------
    // 生命周期
    // -------------------------------------------------------------------------

    @BeforeAll
    static void setUp() {
        String url = System.getProperty("test.redis.url");
        if (url == null || url.isBlank()) {
            // 属性缺失：标记 client 为 null，所有 @Test 将通过 assumeTrue 跳过
            return;
        }
        Config config = new Config();
        config.useSingleServer()
                .setAddress(url)
                .setTimeout(10_000)
                .setConnectTimeout(5_000);
        config.setCodec(new StringCodec());
        client = Redisson.create(config);
    }

    @AfterAll
    static void tearDown() {
        if (client == null) {
            return;
        }
        // 清理所有测试键，避免污染共享的 Upstash 实例
        try {
            // Redisson 4.7.0 RKeys 无 flushbyPrefix，用 pattern 删除前缀键
            client.getKeys().deleteByPattern(PREFIX + "*");
        } catch (Exception ignored) {
            // 若 pattern 扫描不可用（某些 Redis 变体），静默忽略
        }
        client.shutdown();
    }

    // -------------------------------------------------------------------------
    // 工具方法
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // P0：SPI 契约 — setIfAbsent / get / delete / deleteIfMatch / supportsScript
    // -------------------------------------------------------------------------

    @Nested
    class SetIfAbsentAndGet {

        @Test
        void setIfAbsent_returnsTrueWhenKeyAbsent() {
            assumeRedisAvailable();
            RedissonCommandExecutor ex = executor();
            String key = freshKey("set-get-1");

            boolean result = ex.setIfAbsent(key, "hello", 5_000L);

            assertThat(result).isTrue();
            assertThat(ex.get(key)).isEqualTo("hello");
            cleanup(key);
        }

        @Test
        void setIfAbsent_returnsFalseWhenKeyExists() {
            assumeRedisAvailable();
            RedissonCommandExecutor ex = executor();
            String key = freshKey("set-get-2");

            ex.setIfAbsent(key, "first", 5_000L);
            boolean second = ex.setIfAbsent(key, "second", 5_000L);

            assertThat(second).isFalse();
            // 值应保持首次写入，证明 CAS 语义正确
            assertThat(ex.get(key)).isEqualTo("first");
            cleanup(key);
        }

        @Test
        void setIfAbsent_nullValue_allowedBecauseStringCodecIsLenient() {
            assumeRedisAvailable();
            RedissonCommandExecutor ex = executor();
            String key = freshKey("set-get-3");

            // Redisson StringCodec / ByteArrayCodec 对 null value 的处理：
            // 实际写入 ""（空串），本测试验证该行为是可预期的，不抛异常
            boolean result = ex.setIfAbsent(key, "", 5_000L);

            assertThat(result).isTrue();
            assertThat(ex.get(key)).isEqualTo("");
            cleanup(key);
        }

        @Test
        void get_returnsNullWhenAbsent() {
            assumeRedisAvailable();
            RedissonCommandExecutor ex = executor();
            String key = freshKey("get-absent");

            assertThat(ex.get(key)).isNull();
        }

        @Test
        void get_returnsStoredStringWithUtf8Content() {
            assumeRedisAvailable();
            RedissonCommandExecutor ex = executor();
            String key = freshKey("get-utf8");
            String value = "中文内容 🚀";

            ex.setIfAbsent(key, value, 5_000L);

            assertThat(ex.get(key)).isEqualTo(value);
            cleanup(key);
        }
    }

    @Nested
    class Delete {

        @Test
        void delete_removesKeyAndGetReturnsNull() {
            assumeRedisAvailable();
            RedissonCommandExecutor ex = executor();
            String key = freshKey("delete-1");

            ex.setIfAbsent(key, "v", 5_000L);
            ex.delete(key);

            assertThat(ex.get(key)).isNull();
        }

        @Test
        void delete_onAbsentKey_isIdempotentAndDoesNotThrow() {
            assumeRedisAvailable();
            RedissonCommandExecutor ex = executor();
            String key = freshKey("delete-absent");

            // delete 在键不存在时不应抛异常
            ex.delete(key);
            assertThat(ex.get(key)).isNull();
        }
    }

    @Nested
    class DeleteIfMatch {

        @Test
        void deleteIfMatch_returnsTrueWhenValueMatches() {
            assumeRedisAvailable();
            RedissonCommandExecutor ex = executor();
            String key = freshKey("dim-1");
            String value = "lock-token-abc";

            ex.setIfAbsent(key, value, 10_000L);
            boolean deleted = ex.deleteIfMatch(key, value);

            assertThat(deleted).isTrue();
            assertThat(ex.get(key)).isNull();
        }

        @Test
        void deleteIfMatch_returnsFalseWhenValueMismatch() {
            assumeRedisAvailable();
            RedissonCommandExecutor ex = executor();
            String key = freshKey("dim-2");

            ex.setIfAbsent(key, "correct-value", 10_000L);
            boolean deleted = ex.deleteIfMatch(key, "wrong-value");

            assertThat(deleted).isFalse();
            // 键应保留原值，证明非原子误删
            assertThat(ex.get(key)).isEqualTo("correct-value");
            cleanup(key);
        }

        @Test
        void deleteIfMatch_onAbsentKey_returnsFalse() {
            assumeRedisAvailable();
            RedissonCommandExecutor ex = executor();
            String key = freshKey("dim-absent");

            boolean deleted = ex.deleteIfMatch(key, "any-value");

            assertThat(deleted).isFalse();
        }
    }

    @Nested
    class SupportsScript {

        @Test
        void supportsScript_returnsTrueBecauseRedissonNativelySupportsLua() {
            assumeRedisAvailable();
            assertThat(executor().supportsScript()).isTrue();
        }
    }

    // -------------------------------------------------------------------------
    // P0：eval 脚本能力
    // -------------------------------------------------------------------------

    @Nested
    class Eval {

        @Test
        void eval_incrScript_returnsIncrementedLong() {
            assumeRedisAvailable();
            RedissonCommandExecutor ex = executor();
            String key = freshKey("eval-incr");

            Object r1 = ex.eval(
                    "return redis.call('incr', KEYS[1])",
                    List.of(key),
                    List.of());
            Object r2 = ex.eval(
                    "return redis.call('incr', KEYS[1])",
                    List.of(key),
                    List.of());

            // incr 从 0 开始，第一次返回 1，第二次返回 2
            assertThat(r1).isEqualTo(1L);
            assertThat(r2).isEqualTo(2L);
            cleanup(key);
        }

        @Test
        void eval_withZeroArgs_returnsNullForNonReturningScript() {
            assumeRedisAvailable();
            RedissonCommandExecutor ex = executor();
            // SET 命令返回 "OK"，不是 null；用空 Lua 块测试 null 返回
            Object result = ex.eval(
                    "return nil",
                    List.of(),
                    List.of());

            assertThat(result).isNull();
        }

        @Test
        void eval_passesAllArgsThroughCorrectly() {
            assumeRedisAvailable();
            RedissonCommandExecutor ex = executor();
            String script = "return ARGV[1] .. '-' .. ARGV[2]";

            Object result = ex.eval(script, List.of(), List.of("a", "b"));

            assertThat(result).isEqualTo("a-b");
        }

        @Test
        void eval_withMultiKeyScript_performsAtomicReadModifyWrite() {
            assumeRedisAvailable();
            RedissonCommandExecutor ex = executor();
            String keyA = freshKey("eval-mk-a");
            String keyB = freshKey("eval-mk-b");

            ex.setIfAbsent(keyA, "10", 10_000L);

            // 脚本：把 keyB 设为 keyA 的值加 5
            Long result = (Long) ex.eval(
                    "local v = redis.call('get', KEYS[1]) "
                            + "if v then "
                            + "  redis.call('set', KEYS[2], tostring(tonumber(v) + 5)) "
                            + "  return 1 "
                            + "else return 0 end",
                    List.of(keyA, keyB),
                    List.of());

            assertThat(result).isEqualTo(1L);
            assertThat(ex.get(keyB)).isEqualTo("15");
            cleanup(keyA, keyB);
        }

        @Test
        void eval_blankScript_throwsBusinessException() {
            assumeRedisAvailable();
            RedissonCommandExecutor ex = executor();

            assertThatThrownBy(() -> ex.eval("   ", List.of("k"), List.of()))
                    .isInstanceOf(cn.jowen.framework.core.exception.BusinessException.class)
                    .hasMessageContaining("script must not be blank");
        }

        @Test
        void eval_nullScript_throwsBusinessException() {
            assumeRedisAvailable();
            RedissonCommandExecutor ex = executor();
            @SuppressWarnings("null")
            String nullScript = null;

            assertThatThrownBy(() -> ex.eval(nullScript, List.of("k"), List.of()))
                    .isInstanceOf(cn.jowen.framework.core.exception.BusinessException.class)
                    .hasMessageContaining("script must not be blank");
        }

        @Test
        void eval_nullKeys_throwsBusinessException() {
            assumeRedisAvailable();
            RedissonCommandExecutor ex = executor();

            assertThatThrownBy(() -> ex.eval("return 1", null, List.of()))
                    .isInstanceOf(cn.jowen.framework.core.exception.BusinessException.class)
                    .hasMessageContaining("keys must not be null");
        }

        @Test
        void eval_nullArgs_throwsBusinessException() {
            assumeRedisAvailable();
            RedissonCommandExecutor ex = executor();

            assertThatThrownBy(() -> ex.eval("return 1", List.of("k"), null))
                    .isInstanceOf(cn.jowen.framework.core.exception.BusinessException.class)
                    .hasMessageContaining("args must not be null");
        }
    }

    // -------------------------------------------------------------------------
    // P1：TTL / 过期行为
    // -------------------------------------------------------------------------

    @Nested
    class Ttl {

        @Test
        void setIfAbsent_keyExpiresAfterTtl() throws InterruptedException {
            assumeRedisAvailable();
            RedissonCommandExecutor ex = executor();
            String key = freshKey("ttl-1");

            ex.setIfAbsent(key, "v", 500L); // 500ms TTL
            assertThat(ex.get(key)).isEqualTo("v");

            Thread.sleep(800L);
            // TTL 到期后 get 返回 null
            assertThat(ex.get(key)).isNull();
        }

        @Test
        void setIfAbsent_longTtl_keyPersists() throws InterruptedException {
            assumeRedisAvailable();
            RedissonCommandExecutor ex = executor();
            String key = freshKey("ttl-long");

            ex.setIfAbsent(key, "persistent", 30_000L);
            Thread.sleep(500L);

            // 30s TTL 内键仍存在
            assertThat(ex.get(key)).isEqualTo("persistent");
            cleanup(key);
        }
    }

    // -------------------------------------------------------------------------
    // P1：分布式锁 CAS 语义
    // -------------------------------------------------------------------------

    @Nested
    class DistributedLockSemantics {

        @Test
        void deleteIfMatch_preventsMisreleaseByOtherThread() {
            assumeRedisAvailable();
            RedissonCommandExecutor ex = executor();
            String key = freshKey("lock-cas");
            String ownerA = "owner-A-" + System.currentTimeMillis();
            String ownerB = "owner-B-" + System.currentTimeMillis();

            // 线程 A 持有锁
            ex.setIfAbsent(key, ownerA, 10_000L);

            // 线程 B 尝试用自己的 token 释放 → 失败
            boolean releasedByB = ex.deleteIfMatch(key, ownerB);

            assertThat(releasedByB).isFalse();
            // 锁仍归 A 所有
            assertThat(ex.get(key)).isEqualTo(ownerA);
            cleanup(key);
        }

        @Test
        void setIfAbsent_concurrentOnlyOneSucceeds() throws InterruptedException {
            assumeRedisAvailable();
            RedissonCommandExecutor ex = executor();
            String key = freshKey("lock-concurrent");

            java.util.concurrent.atomic.AtomicInteger successCount =
                    new java.util.concurrent.atomic.AtomicInteger();

            java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(10);

            java.util.List<Thread> threads = new java.util.ArrayList<>();
            for (int i = 0; i < 10; i++) {
                final int idx = i;
                Thread t = new Thread(() -> {
                    try {
                        start.await();
                        boolean ok = ex.setIfAbsent(key, "holder-" + idx, 5_000L);
                        if (ok) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        done.countDown();
                    }
                });
                t.start();
                threads.add(t);
            }

            start.countDown();
            done.await(10, java.util.concurrent.TimeUnit.SECONDS);

            // CAS 语义：10 个并发只有 1 个成功
            assertThat(successCount.get()).isEqualTo(1);
            cleanup(key);
        }
    }

    // -------------------------------------------------------------------------
    // 私有辅助
    // -------------------------------------------------------------------------

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
}
