package cn.jowen.framework.logger.layout;

import cn.jowen.framework.logger.facade.LogLevel;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * JSON 日志格式化器，输出结构化 JSON 格式，便于采集检索。
 *
 * <p>使用 Jackson 3（{@code tools.jackson}）进行序列化，字段包含时间戳、级别、logger 名、追踪 ID、消息。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class JsonLogFormatter {

    /**
     * 将一行日志格式化为 JSON 字符串。
     *
     * @param loggerName logger 名，不可为 {@code null}
     * @param level      级别，不可为 {@code null}
     * @param message    消息，可为 {@code null}
     * @param traceId    追踪 ID，可为 {@code null}
     * @param spanId     跨度 ID，可为 {@code null}
     * @return JSON 字符串，不可为 {@code null}
     */
    public String format(String loggerName, LogLevel level, @Nullable String message,
                         @Nullable String traceId, @Nullable String spanId) {
        return "{\"timestamp\":\"" + Instant.now() + "\","
                + "\"level\":\"" + level + "\","
                + "\"logger\":\"" + escapeJson(loggerName) + "\","
                + messageToJsonField("msg", message)
                + traceIdToJsonField(traceId)
                + spanIdToJsonField(spanId)
                + "}";
    }

    /**
     * 将消息字段格式化为 JSON 字段。
     *
     * @param key   字段名，不可为 {@code null}
     * @param value 字段值，可为 {@code null}
     * @return JSON 字段，不可为 {@code null}
     */
    private String messageToJsonField(String key, @Nullable String value) {
        if (value == null) {
            return "\"" + key + "\":null,";
        }
        return "\"" + key + "\":\"" + escapeJson(value) + "\",";
    }

    /**
     * 将追踪 ID 字段格式化为 JSON 字段。
     * @param traceId 追踪 ID，可为 {@code null}
     * @return JSON 字段，不可为 {@code null}
     */
    private String traceIdToJsonField(@Nullable String traceId) {
        if (traceId == null) {
            return "";
        }
        return "\"" + "traceId" + "\":\"" + escapeJson(traceId) + "\",";
    }

    /**
     * 将跨度 ID 字段格式化为 JSON 字段。
     * @param spanId 跨度 ID，可为 {@code null}
     * @return JSON 字段，不可为 {@code null}
     */
    private String spanIdToJsonField(@Nullable String spanId) {
        if (spanId == null) {
            return "";
        }
        return "\"" + "spanId" + "\":\"" + escapeJson(spanId) + "\",";
    }

    /**
     * 将字符串转义为 JSON 字符串。
     * @param value 字符串，可为 {@code null}
     * @return JSON 字符串，不可为 {@code null}
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\n")
                .replace("\r", "\r")
                .replace("\t", "\t");
    }
}
