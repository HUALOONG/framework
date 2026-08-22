package cn.jowen.framework.extras;

import cn.jowen.framework.cache.CacheBuilder;
import cn.jowen.framework.extras.idempotent.Idempotent;
import cn.jowen.framework.extras.lock.LocalLock;
import cn.jowen.framework.extras.ratelimit.RateLimiter;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ExtrasTest {

    @Test
    void rateLimiterAllowsWithinCapacity() {
        RateLimiter limiter = new RateLimiter(100, 5); // 5 容量，100/s
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryAcquire()).isTrue();
        }
        assertThat(limiter.tryAcquire()).isFalse(); // 桶空
    }

    @Test
    void idempotentBlocksDuplicate() {
        Idempotent idempotent = Idempotent.of(
                CacheBuilder.<String, Boolean>local("idem", 0L).build(), 1, TimeUnit.MINUTES);
        assertThat(idempotent.tryOccupy("order-1")).isTrue();
        assertThat(idempotent.tryOccupy("order-1")).isFalse();
        idempotent.release("order-1");
        assertThat(idempotent.tryOccupy("order-1")).isTrue();
    }

    @Test
    void localLockReentrantAndUnlock() throws InterruptedException {
        LocalLock lock = new LocalLock();
        assertThat(lock.tryLock()).isTrue();
        assertThat(lock.tryLock()).isTrue(); // ReentrantLock 可重入
        lock.unlock();
        lock.unlock();
        // 释放后其他线程可获取
        assertThat(lock.tryLock()).isTrue();
        lock.unlock();
    }
}
