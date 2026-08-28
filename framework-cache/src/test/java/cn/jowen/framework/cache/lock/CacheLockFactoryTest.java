package cn.jowen.framework.cache.lock;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * {@link CacheLockFactory} 测试。
 */
class CacheLockFactoryTest {

    private final RedissonClient client = mock(RedissonClient.class);
    private final CacheLockFactory factory = new CacheLockFactory(client);

    @Test
    void getLock_returnsSameInstanceForSameName() {
        assertThat(factory.getLock("users")).isSameAs(factory.getLock("users"));
    }

    @Test
    void getLock_differentNamesReturnDifferentLocks() {
        assertThat(factory.getLock("a")).isNotSameAs(factory.getLock("b"));
    }

    @Test
    void releaseLock_null_doesNotThrow() {
        assertThatCode(() -> factory.releaseLock(null)).doesNotThrowAnyException();
    }

    @Test
    void releaseLock_unlocksGivenLock() {
        CacheLock lock = mock(CacheLock.class);
        factory.releaseLock(lock);
        verify(lock).unlock();
    }
}
