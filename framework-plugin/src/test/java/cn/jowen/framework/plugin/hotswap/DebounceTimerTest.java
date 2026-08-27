package cn.jowen.framework.plugin.hotswap;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DebounceTimerTest {

    @Test
    void runOnce_executesRunnable() throws InterruptedException {
        DebounceTimer timer = new DebounceTimer(100);
        AtomicInteger count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        timer.runOnce(() -> {
            count.incrementAndGet();
            latch.countDown();
        });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(count.get()).isEqualTo(1);
        timer.shutdown();
    }

    @Test
    void runOnce_multipleCalls_onlyExecutesOnce() throws InterruptedException {
        DebounceTimer timer = new DebounceTimer(200);
        AtomicInteger count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        for (int i = 0; i < 10; i++) {
            timer.runOnce(() -> {
                count.incrementAndGet();
                latch.countDown();
            });
        }
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(count.get()).isEqualTo(1);
        timer.shutdown();
    }

    @Test
    void shutdown_stopsScheduler() {
        DebounceTimer timer = new DebounceTimer(100);
        timer.shutdown();
    }
}
