package cn.jowen.framework.extras.web.idempotent;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LocalIdempotentStore} 防重指纹验证。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
class LocalIdempotentStoreTest {

    @Test
    void firstMarkSucceedsSecondFails() {
        LocalIdempotentStore store = new LocalIdempotentStore();
        assertThat(store.tryMark("k1", 60, TimeUnit.SECONDS)).isTrue();
        assertThat(store.tryMark("k1", 60, TimeUnit.SECONDS)).isFalse();
    }

    @Test
    void removeAllowsReMark() {
        LocalIdempotentStore store = new LocalIdempotentStore();
        store.tryMark("k", 60, TimeUnit.SECONDS);
        store.remove("k");
        assertThat(store.tryMark("k", 60, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void distinctKeysAreIndependent() {
        LocalIdempotentStore store = new LocalIdempotentStore();
        assertThat(store.tryMark("a", 60, TimeUnit.SECONDS)).isTrue();
        assertThat(store.tryMark("b", 60, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void expiredMarkCanBeMarkedAgain() throws Exception {
        LocalIdempotentStore store = new LocalIdempotentStore();
        assertThat(store.tryMark("short", 1, TimeUnit.MILLISECONDS)).isTrue();
        Thread.sleep(20L);
        assertThat(store.tryMark("short", 1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void removeUnknownKeyIsNoop() {
        LocalIdempotentStore store = new LocalIdempotentStore();
        store.remove("never-existed");
        assertThat(store.tryMark("never-existed", 60, TimeUnit.SECONDS)).isTrue();
    }
}
