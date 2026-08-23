package cn.jowen.framework.logger.trace;

import cn.jowen.framework.core.context.ContextCarrier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TraceContext} 测试。
 */
class TraceContextTest {

    private final ContextCarrier.Mode originalMode;

    TraceContextTest() {
        this.originalMode = ContextCarrier.mode();
    }

    @BeforeEach
    void setUp() throws Exception {
        ContextCarrier.configure(ContextCarrier.Mode.THREAD_LOCAL);
        // 清除之前的 traceId/spanId
        try (AutoCloseable scope = TraceContext.open("cleanup", "cleanup")) {}
    }

    @AfterEach
    void tearDown() throws Exception {
        // 先恢复到 ThreadLocal 模式，清理 ThreadLocal 中的值，再恢复原始模式
        ContextCarrier.configure(ContextCarrier.Mode.THREAD_LOCAL);
        try (AutoCloseable scope = TraceContext.open("cleanup", "cleanup")) {}
        ContextCarrier.configure(originalMode);
    }

    @Test
    void getTraceId_nullWhenNotOpen() {
        assertThat(TraceContext.getTraceId()).isNull();
    }

    @Test
    void getSpanId_nullWhenNotOpen() {
        assertThat(TraceContext.getSpanId()).isNull();
    }

    @Test
    void open_generatesTraceIdAndSpanId() throws Exception {
        try (AutoCloseable scope = TraceContext.open()) {
            String traceId = TraceContext.getTraceId();
            String spanId = TraceContext.getSpanId();
            assertThat(traceId).isNotNull();
            assertThat(spanId).isNotNull();
            assertThat(traceId).isNotEmpty();
            assertThat(spanId).isNotEmpty();
        }
    }

    @Test
    void open_withTraceId_only() throws Exception {
        String fixedTraceId = "my-trace-id-123";
        try (AutoCloseable scope = TraceContext.open(fixedTraceId)) {
            assertThat(TraceContext.getTraceId()).isEqualTo(fixedTraceId);
            assertThat(TraceContext.getSpanId()).isNotNull();
        }
    }

    @Test
    void open_withTraceIdAndSpanId() throws Exception {
        String traceId = "fixed-trace";
        String spanId = "fixed-span";
        try (AutoCloseable scope = TraceContext.open(traceId, spanId)) {
            assertThat(TraceContext.getTraceId()).isEqualTo(traceId);
            assertThat(TraceContext.getSpanId()).isEqualTo(spanId);
        }
    }

    @Test
    void open_closedScope_clearsValues() throws Exception {
        try (AutoCloseable scope = TraceContext.open("active-trace", "active-span")) {
            assertThat(TraceContext.getTraceId()).isEqualTo("active-trace");
        }
        assertThat(TraceContext.getTraceId()).isNull();
        assertThat(TraceContext.getSpanId()).isNull();
    }

    @Test
    void nestedOpen_innerOverridesOuter() throws Exception {
        try (AutoCloseable outer = TraceContext.open("outer-trace", "outer-span")) {
            assertThat(TraceContext.getTraceId()).isEqualTo("outer-trace");
            try (AutoCloseable inner = TraceContext.open("inner-trace", "inner-span")) {
                assertThat(TraceContext.getTraceId()).isEqualTo("inner-trace");
                assertThat(TraceContext.getSpanId()).isEqualTo("inner-span");
            }
            // 已知行为：内部作用域关闭后值为 null，外层不自动恢复
            assertThat(TraceContext.getTraceId()).isNull();
        }
        assertThat(TraceContext.getTraceId()).isNull();
    }

    @Test
    void open_multipleTimes_generatesDifferentIds() throws Exception {
        String firstTrace = null;
        String secondTrace = null;
        try (AutoCloseable s1 = TraceContext.open()) {
            firstTrace = TraceContext.getTraceId();
        }
        try (AutoCloseable s2 = TraceContext.open()) {
            secondTrace = TraceContext.getTraceId();
        }
        assertThat(firstTrace).isNotEqualTo(secondTrace);
    }

    @Test
    void traceId_formatIsUuidLike() throws Exception {
        try (AutoCloseable scope = TraceContext.open()) {
            String traceId = TraceContext.getTraceId();
            // UUID 格式：xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
            assertThat(traceId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }
    }
}
