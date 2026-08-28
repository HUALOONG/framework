package cn.jowen.framework.logger.trace;

import cn.jowen.framework.core.context.ContextCarrier;
import cn.jowen.framework.core.context.ContextKey;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * 追踪上下文，持有 {@code traceId} / {@code spanId}，读写委托 {@link ContextCarrier}。
 *
 * <p>通过 {@link #open()} 进入作用域后，所有日志自动携带当前链路 ID，虚拟线程切换不丢失。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class TraceContext {
    /**
     * 追踪 ID 上下文键，不可为 {@code null}
     */
    private static final ContextKey<String> TRACE_ID = ContextKey.named("traceId", String.class);

    /**
     * 跨度 ID 上下文键，不可为 {@code null}
     */
    private static final ContextKey<String> SPAN_ID = ContextKey.named("spanId", String.class);

    private TraceContext() { }

    /**
     * 生成新的追踪上下文作用域，返回 {@link AutoCloseable} 供 try-with-resources 使用。
     *
     * @return 自动关闭的作用域，不可为 {@code null}
     */
    public static AutoCloseable open() {
        return open(UUID.randomUUID().toString());
    }

    /**
     * 使用已有 {@code traceId} 进入追踪上下文作用域。
     *
     * @param traceId 追踪 ID，不可为 {@code null}
     * @return 自动关闭的作用域，不可为 {@code null}
     */
    public static AutoCloseable open(String traceId) {
        return open(traceId, UUID.randomUUID().toString());
    }

    /**
     * 使用已有 {@code traceId} / {@code spanId} 进入追踪上下文作用域。
     *
     * @param traceId 追踪 ID，不可为 {@code null}
     * @param spanId  跨度 ID，不可为 {@code null}
     * @return 自动关闭的作用域，不可为 {@code null}
     */
    public static AutoCloseable open(String traceId, String spanId) {
        ContextCarrier.set(TRACE_ID, traceId);
        ContextCarrier.set(SPAN_ID, spanId);
        return () -> {
            ContextCarrier.set(TRACE_ID, null);
            ContextCarrier.set(SPAN_ID, null);
        };
    }

    /**
     * 获取当前追踪 ID，未设置时返回 {@code null}。
     *
     * @return 追踪 ID，可为 {@code null}
     */
    @Nullable
    public static String getTraceId() {
        return ContextCarrier.get(TRACE_ID);
    }

    /**
     * 获取当前跨度 ID，未设置时返回 {@code null}。
     *
     * @return 跨度 ID，可为 {@code null}
     */
    @Nullable
    public static String getSpanId() {
        return ContextCarrier.get(SPAN_ID);
    }
}
