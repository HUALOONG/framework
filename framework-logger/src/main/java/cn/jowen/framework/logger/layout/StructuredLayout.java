package cn.jowen.framework.logger.layout;

import cn.jowen.framework.logger.facade.LogLevel;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Arrays;

/**
 * 结构化日志布局。当前提供文本行格式（含时间戳/级别/logger/消息），JSON 输出待 Jackson 3 集成后扩展。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class StructuredLayout {

    /**
     * 将一行日志格式化为文本。占位符 {@code {} } 由调用方已填充。
     *
     * @param loggerName logger 名，不可为 {@code null}
     * @param level      级别，不可为 {@code null}
     * @param message    消息（已填充占位符），可为 {@code null}
     * @param args       原始参数，可为 {@code null}
     * @return 格式化文本，不可为 {@code null}
     */
    public String formatText(String loggerName, LogLevel level, @Nullable String message, @Nullable Object[] args) {
        String msg = message != null ? message : "";
        if (args.length > 0) {
            msg = fillPlaceholders(msg, args);
        }
        return String.format("%s [%s] %s - %s%n", Instant.now(), level, loggerName, msg);
    }

    private static String fillPlaceholders(String template, Object[] args) {
        StringBuilder sb = new StringBuilder();
        int argIdx = 0;
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '{' && i + 1 < template.length() && template.charAt(i + 1) == '}') {
                if (argIdx < args.length) {
                    sb.append(args[argIdx++]);
                }
                i += 2;
                continue;
            }
            sb.append(c);
            i++;
        }
        if (argIdx < args.length) {
            sb.append(' ').append(Arrays.toString(Arrays.copyOfRange(args, argIdx, args.length)));
        }
        return sb.toString();
    }
}
