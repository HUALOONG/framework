package cn.jowen.framework.logger.trace;

import cn.jowen.framework.core.context.ContextCarrier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link MdcContextPropagation} 测试。
 */
class MdcContextPropagationTest {

    private final ContextCarrier.Mode originalMode;
    private final AtomicReference<String> captured = new AtomicReference<>();

    MdcContextPropagationTest() {
        this.originalMode = ContextCarrier.mode();
    }

    @BeforeEach
    void setUp() throws Exception {
        ContextCarrier.configure(ContextCarrier.Mode.THREAD_LOCAL);
        try (AutoCloseable scope = TraceContext.open("cleanup", "cleanup")) {
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        ContextCarrier.configure(ContextCarrier.Mode.THREAD_LOCAL);
        try (AutoCloseable scope = TraceContext.open("cleanup", "cleanup")) {
        }
        ContextCarrier.configure(originalMode);
    }

    @Test
    void wrap_carriesTraceContextToAnotherThread() throws Exception {
        MdcContextPropagation propagation = new MdcContextPropagation();
        try (AutoCloseable scope = TraceContext.open("trace-1", "span-1")) {
            Runnable wrapped = propagation.wrap(() ->
                    captured.set(TraceContext.getTraceId() + "/" + TraceContext.getSpanId()));
            Thread thread = new Thread(wrapped);
            thread.start();
            thread.join();
        }
        assertThat(captured.get()).isEqualTo("trace-1/span-1");
    }

    @Test
    void wrap_withoutContext_doesNotThrow() {
        Runnable wrapped = new MdcContextPropagation().wrap(() -> {
        });
        assertThatCode(wrapped::run).doesNotThrowAnyException();
    }

    @Test
    void wrap_taskExecutesInCallingThread() {
        MdcContextPropagation propagation = new MdcContextPropagation();
        AtomicReference<Thread> executedOn = new AtomicReference<>();
        Runnable wrapped = propagation.wrap(() -> executedOn.set(Thread.currentThread()));
        wrapped.run();
        assertThat(executedOn.get()).isSameAs(Thread.currentThread());
    }
}
