package cn.jowen.framework.logger.trace;

import cn.jowen.framework.core.context.ContextCarrier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TraceEnhancer} 测试。
 */
class TraceEnhancerTest {

    private final ContextCarrier.Mode originalMode;

    TraceEnhancerTest() {
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
    void enhance_returnsEmpty_whenNoContextAndNullMessage() {
        assertThat(TraceEnhancer.enhance(null)).isEmpty();
    }

    @Test
    void enhance_returnsMessage_whenNoContext() {
        assertThat(TraceEnhancer.enhance("hello")).isEqualTo("hello");
    }

    @Test
    void enhance_prependsTraceAndSpan() throws Exception {
        try (AutoCloseable scope = TraceContext.open("trace-x", "span-y")) {
            assertThat(TraceEnhancer.enhance("msg")).isEqualTo("[traceId=trace-x][spanId=span-y] msg");
        }
    }

    @Test
    void enhance_withContextAndNullMessage_returnsPrefix() throws Exception {
        try (AutoCloseable scope = TraceContext.open("trace-x", "span-y")) {
            assertThat(TraceEnhancer.enhance(null)).isEqualTo("[traceId=trace-x][spanId=span-y] ");
        }
    }

    @Test
    void isInTraceContext_falseWithoutContext() {
        assertThat(TraceEnhancer.isInTraceContext()).isFalse();
    }

    @Test
    void isInTraceContext_trueWithContext() throws Exception {
        try (AutoCloseable scope = TraceContext.open("t")) {
            assertThat(TraceEnhancer.isInTraceContext()).isTrue();
        }
    }

    @Test
    void traceIdAndSpanId_emptyWithoutContext() {
        assertThat(TraceEnhancer.traceId()).isEmpty();
        assertThat(TraceEnhancer.spanId()).isEmpty();
    }

    @Test
    void traceIdAndSpanId_presentWithContext() throws Exception {
        try (AutoCloseable scope = TraceContext.open("t-1", "s-1")) {
            assertThat(TraceEnhancer.traceId()).contains("t-1");
            assertThat(TraceEnhancer.spanId()).contains("s-1");
        }
    }
}
