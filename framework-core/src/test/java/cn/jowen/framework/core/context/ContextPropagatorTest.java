package cn.jowen.framework.core.context;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * {@link ContextPropagator} 测试。
 */
class ContextPropagatorTest {

    private static final ContextPropagator IDENTITY = new ContextPropagator() {
        @Override
        public Runnable wrap(Runnable task) {
            return task;
        }
    };

    @Test
    void wrapRunnable_delegates() {
        boolean[] executed = {false};
        Runnable runnable = () -> executed[0] = true;
        IDENTITY.wrap(runnable).run();
        assertThat(executed[0]).isTrue();
    }

    @Test
    void wrapCallable_success_returnsResult() throws Exception {
        Callable<String> wrapped = IDENTITY.wrap(() -> "result");
        assertThat(wrapped.call()).isEqualTo("result");
    }

    @Test
    void wrapCallable_runtimeException_passedThrough() throws Exception {
        Callable<String> wrapped = IDENTITY.wrap(() -> { throw new RuntimeException("boom"); });

        try {
            wrapped.call();
            fail("should throw");
        } catch (RuntimeException ex) {
            assertThat(ex).hasMessage("boom");
        }
    }

    @Test
    void wrapCallable_checkedException_wrappedInWrappedExecutionException() throws Exception {
        Callable<String> wrapped = IDENTITY.wrap(() -> { throw new Exception("io error"); });

        try {
            wrapped.call();
            fail("should throw");
        } catch (RuntimeException ex) {
            assertThat(ex).isInstanceOf(ContextPropagator.WrappedExecutionException.class);
            assertThat(ex.getCause()).isInstanceOf(Exception.class);
            assertThat(ex.getCause()).hasMessage("io error");
        }
    }

    @Test
    void wrapCallable_replaysContext() {
        ContextKey<String> key = ContextKey.named("test", String.class);
        String[] result = {null};
        ContextCarrier.runWith(key, "before", () -> {
            assertThat(ContextCarrier.get(key)).isEqualTo("before");
            Callable<String> wrapped = IDENTITY.wrap(() -> ContextCarrier.get(key));
            try {
                result[0] = wrapped.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertThat(result[0]).isEqualTo("before");
        assertThat(ContextCarrier.get(key)).isNull();
    }

    @Test
    void wrapCallable_crossThread_contextPropagation() {
        ContextKey<String> key = ContextKey.named("ctx", String.class);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        AtomicReference<String> resultRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        ContextCarrier.runWith(key, "fromMain", () -> {
            Callable<String> wrapped = IDENTITY.wrap(() -> {
                String captured = ContextCarrier.get(key);
                resultRef.set(captured);
                latch.countDown();
                return captured;
            });
            executor.submit(wrapped);
        });

        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertThat(resultRef.get()).isEqualTo("fromMain");
        executor.shutdown();
    }
}
