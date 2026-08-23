package cn.jowen.framework.logger.trace;

import cn.jowen.framework.logger.facade.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * TraceId 注入增强器，在日志级别判断时同步注入 traceId/spanId 占位符，保证日志输出携带链路信息。
 *
 * <p>使用方式：业务代码只需调用 {@link #enhance(Logger, String)} 获取增强后的日志信息字符串。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class TraceEnhancer {
    /**
     * 追踪 ID 上下文键，不可为 {@code null}
     */
    private static final String TRACE_KEY = "traceId";

    /**
     * 跨度 ID 上下文键，不可为 {@code null}
     */
    private static final String SPAN_KEY = "spanId";

    private TraceEnhancer() { }

    /**
     * 在日志消息前注入追踪上下文，返回增强后的消息字符串。
     *
     * @param message 原始消息，可为 {@code null}
     * @return 增强后的消息，不为 {@code null}
     */
    public static String enhance(@Nullable String message) {
        String traceId = TraceContext.getTraceId();
        String spanId = TraceContext.getSpanId();
        if (traceId == null && spanId == null) {
            return message != null ? message : "";
        }
        StringBuilder sb = new StringBuilder();
        if (traceId != null) {
            sb.append('[').append(TRACE_KEY).append('=').append(traceId).append(']');
        }
        if (spanId != null) {
            sb.append('[').append(SPAN_KEY).append('=').append(spanId).append(']');
        }
        sb.append(' ');
        if (message != null) {
            sb.append(message);
        }
        return sb.toString();
    }

    /**
     * 判断当前是否在追踪上下文中（traceId 已设置）。
     *
     * @return 在追踪上下文中返回 {@code true}
     */
    public static boolean isInTraceContext() {
        return TraceContext.getTraceId() != null;
    }

    /**
     * 返回当前追踪 ID，若不存在返回 {@link Optional#empty()}。
     *
     * @return 追踪 ID
     */
    public static Optional<String> traceId() {
        return Optional.ofNullable(TraceContext.getTraceId());
    }

    /**
     * 返回当前跨度 ID，若不存在返回 {@link Optional#empty()}。
     *
     * @return 跨度 ID
     */
    public static Optional<String> spanId() {
        return Optional.ofNullable(TraceContext.getSpanId());
    }
}
