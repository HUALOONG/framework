package cn.jowen.framework.extras.web.captcha;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link CaptchaStore.InMemory} 测试。
 *
 * <p>覆盖：保存/读取/移除、同 id 覆盖、未知 id 返回 null、移除未知 id 幂等、
 * 多 key 隔离、并发读写不丢数据。
 */
class CaptchaStoreInMemoryTest {

    private CaptchaStore store;

    @BeforeEach
    void setUp() {
        store = new CaptchaStore.InMemory();
    }

    private static Captcha captcha(String id, String code) {
        return new Captcha(id, code, null, null, System.currentTimeMillis() + 60_000L);
    }

    @Test
    void save_thenGet_returnsSameInstance() {
        Captcha captcha = captcha("id-1", "1234");
        store.save(captcha);

        assertThat(store.get("id-1")).isSameAs(captcha);
    }

    @Test
    void get_returnsNullForUnknownId() {
        assertThat(store.get("nope")).isNull();
    }

    @Test
    void get_doesNotConsumeEntry() {
        store.save(captcha("id-1", "1234"));

        assertThat(store.get("id-1")).isNotNull();
        assertThat(store.get("id-1")).as("get 不应移除条目").isNotNull();
    }

    @Test
    void save_withSameIdOverwritesPrevious() {
        store.save(captcha("id-1", "old"));
        store.save(captcha("id-1", "new"));

        Captcha stored = store.get("id-1");
        assertThat(stored).isNotNull();
        assertThat(stored.code()).isEqualTo("new");
    }

    @Test
    void remove_deletesEntry() {
        store.save(captcha("id-1", "1234"));
        store.remove("id-1");

        assertThat(store.get("id-1")).isNull();
    }

    @Test
    void remove_unknownIdIsNoop() {
        assertThatCode(() -> store.remove("never-existed")).doesNotThrowAnyException();
    }

    @Test
    void entriesAreIsolatedById() {
        store.save(captcha("a", "1"));
        store.save(captcha("b", "2"));

        store.remove("a");

        assertThat(store.get("a")).isNull();
        assertThat(store.get("b")).isNotNull();
    }

    @Test
    void expiredEntriesAreStillReadable_expiryIsCallerConcern() {
        Captcha expired = new Captcha("exp", "1", null, null, System.currentTimeMillis() - 1L);
        store.save(expired);

        // 存储层不做过期清理，过期判定由 CaptchaService 负责
        assertThat(store.get("exp")).isSameAs(expired);
        assertThat(expired.expired()).isTrue();
    }

    @Test
    void concurrentSaveAndGet_losesNothing() throws Exception {
        int threads = 8;
        int perThread = 200;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int t = 0; t < threads; t++) {
                int threadIndex = t;
                pool.execute(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < perThread; i++) {
                            String id = threadIndex + "-" + i;
                            store.save(captcha(id, String.valueOf(i)));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        for (int t = 0; t < threads; t++) {
            for (int i = 0; i < perThread; i++) {
                assertThat(store.get(t + "-" + i)).as("条目 %d-%d 丢失", t, i).isNotNull();
            }
        }
    }
}
