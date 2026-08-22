package cn.jowen.framework.plugin.hotswap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DebounceTimerTest {

    @Test
    @Timeout(5)
    void onlyLastExecutionRuns() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        DebounceTimer timer = new DebounceTimer(100);

        // fire multiple rapid calls
        for (int i = 0; i < 5; i++) {
            timer.runOnce(() -> counter.incrementAndGet());
        }

        Thread.sleep(300);
        timer.shutdown();

        // only the last one should have executed
        assertThat(counter.get()).isLessThanOrEqualTo(1);
    }
}
