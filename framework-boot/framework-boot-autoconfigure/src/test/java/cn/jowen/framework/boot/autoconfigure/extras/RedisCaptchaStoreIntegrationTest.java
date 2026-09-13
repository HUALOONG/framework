package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.extras.web.captcha.Captcha;
import cn.jowen.framework.extras.web.captcha.RedisCaptchaStore;
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

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link RedisCaptchaStore} 集成测试：基于真实 Redis（Upstash）验证验证码存取语义。
 *
 * <p><b>触发条件</b>：必须设置系统属性 {@code -Dtest.redis.url}，否则本类所有用例跳过。
 * 这样 {@code mvn -o clean verify} 默认门禁不会执行任何 Redis 网络调用。
 *
 * <p>典型调用示例：
 * <pre>{@code
 * mvn -o -pl :framework-boot-autoconfigure clean verify \
 *   -Dtest=RedisCaptchaStoreIntegrationTest \
 *   -Dgroups=integration \
 *   -Dtest.redis.url="rediss://default:PASSWORD@host:port"
 * }</pre>
 *
 * <p>密码通过 URL 嵌入，不在源码中硬编码。
 *
 * <p><b>序列化</b>：{@code code / text / image / expireAt} 以 {@code |} 分隔、Base64 编码，
 * 与 {@link RedisCaptchaStore#encode} 一致。
 *
 * @author 王飞
 * @since 0.0.3
 * @version 0.0.3
 */
@NullMarked
@Tag("integration")
class RedisCaptchaStoreIntegrationTest {

    @Nullable
    private static RedissonClient client;

    private static final String PREFIX = "jowen:test:int:captcha:";

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
    void saveAndGetRoundTrip() {
        assumeRedisAvailable();
        String id = freshKey("round-trip");
        long expireAt = System.currentTimeMillis() + 60_000L;
        Captcha captcha = new Captcha(id, "answer-42", "2 + 40", null, expireAt);

        RedisCaptchaStore store = new RedisCaptchaStore(executor());
        store.save(captcha);

        Captcha retrieved = store.get(id);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.id()).isEqualTo(id);
        assertThat(retrieved.code()).isEqualTo("answer-42");
        assertThat(retrieved.text()).isEqualTo("2 + 40");
        assertThat(retrieved.image()).isNull();
        assertThat(retrieved.expireAt()).isEqualTo(expireAt);

        cleanup(id);
    }

    @Test
    void saveAndGetWithImageBytes() {
        assumeRedisAvailable();
        String id = freshKey("with-image");
        long expireAt = System.currentTimeMillis() + 60_000L;
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
        Captcha captcha = new Captcha(id, "img-code", "图形验证码", png, expireAt);

        RedisCaptchaStore store = new RedisCaptchaStore(executor());
        store.save(captcha);

        Captcha retrieved = store.get(id);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.image()).isEqualTo(png);

        cleanup(id);
    }

    @Test
    void saveAndGetWithNullTextAndImage() {
        assumeRedisAvailable();
        String id = freshKey("null-fields");
        long expireAt = System.currentTimeMillis() + 60_000L;
        Captcha captcha = new Captcha(id, "code-only", null, null, expireAt);

        RedisCaptchaStore store = new RedisCaptchaStore(executor());
        store.save(captcha);

        Captcha retrieved = store.get(id);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.text()).isNull();
        assertThat(retrieved.image()).isNull();

        cleanup(id);
    }

    @Test
    void verifyReturnsFalseForWrongCode() {
        assumeRedisAvailable();
        String id = freshKey("wrong-code");
        long expireAt = System.currentTimeMillis() + 60_000L;
        Captcha captcha = new Captcha(id, "correct-code", "题目", null, expireAt);

        RedisCaptchaStore store = new RedisCaptchaStore(executor());
        store.save(captcha);

        Captcha retrieved = store.get(id);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.code()).isNotEqualTo("wrong-code");
        assertThat(retrieved.code()).isEqualTo("correct-code");

        cleanup(id);
    }

    @Test
    void getReturnsNullWhenAbsent() {
        assumeRedisAvailable();
        String id = freshKey("absent");
        RedisCaptchaStore store = new RedisCaptchaStore(executor());

        assertThat(store.get(id)).isNull();
    }

    @Test
    void getReturnsNullAfterTtlExpires() throws InterruptedException {
        assumeRedisAvailable();
        String id = freshKey("expire");
        long expireAt = System.currentTimeMillis() + 500L;
        Captcha captcha = new Captcha(id, "code", "题目", null, expireAt);

        RedisCaptchaStore store = new RedisCaptchaStore(executor());
        store.save(captcha);

        assertThat(store.get(id)).isNotNull();

        Thread.sleep(1_200L);
        assertThat(store.get(id)).isNull();
    }

    @Test
    void removeDeletesKey() {
        assumeRedisAvailable();
        String id = freshKey("remove");
        long expireAt = System.currentTimeMillis() + 60_000L;
        Captcha captcha = new Captcha(id, "code", "题目", null, expireAt);

        RedisCaptchaStore store = new RedisCaptchaStore(executor());
        store.save(captcha);
        store.remove(id);

        assertThat(store.get(id)).isNull();
    }

    @Test
    void distinctIdsAreIndependent() {
        assumeRedisAvailable();
        String idA = freshKey("id-a");
        String idB = freshKey("id-b");
        long now = System.currentTimeMillis();
        Captcha a = new Captcha(idA, "code-a", "题目 A", null, now + 60_000L);
        Captcha b = new Captcha(idB, "code-b", "题目 B", null, now + 60_000L);

        RedisCaptchaStore store = new RedisCaptchaStore(executor());
        store.save(a);
        store.save(b);

        assertThat(store.get(idA).code()).isEqualTo("code-a");
        assertThat(store.get(idB).code()).isEqualTo("code-b");

        cleanup(idA, idB);
    }

    @Test
    void saveRejectsExpiredCaptcha() {
        assumeRedisAvailable();
        String id = freshKey("expired-save");
        long expiredAt = System.currentTimeMillis() - 1_000L;
        Captcha captcha = new Captcha(id, "code", "题目", null, expiredAt);

        RedisCaptchaStore store = new RedisCaptchaStore(executor());

        assertThatThrownBy(() -> store.save(captcha))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already expired");
    }

    @Test
    void saveRejectsNullCaptcha() {
        assumeRedisAvailable();
        RedisCaptchaStore store = new RedisCaptchaStore(executor());

        assertThatThrownBy(() -> store.save(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("captcha must not be null");
    }

    @Test
    void getRejectsNullId() {
        assumeRedisAvailable();
        RedisCaptchaStore store = new RedisCaptchaStore(executor());

        assertThatThrownBy(() -> store.get(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("id must not be null");
    }

    @Test
    void removeRejectsNullId() {
        assumeRedisAvailable();
        RedisCaptchaStore store = new RedisCaptchaStore(executor());

        assertThatThrownBy(() -> store.remove(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("id must not be null");
    }

    @Test
    void constructorRejectsNullExecutor() {
        assumeRedisAvailable();
        assertThatThrownBy(() -> new RedisCaptchaStore(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("executor must not be null");
    }

    @Test
    void constructorRejectsBlankKeyPrefix() {
        assumeRedisAvailable();
        RedisCommandExecutor exec = executor();
        assertThatThrownBy(() -> new RedisCaptchaStore(executor(), "  "))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("keyPrefix must not be blank");
    }
}