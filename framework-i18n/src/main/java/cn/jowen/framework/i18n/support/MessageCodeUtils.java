package cn.jowen.framework.i18n.support;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 消息编码工具：编码规范化（点分/下划线）、前缀拼接、模块命名空间管理。
 * 约定编码格式为 {@code 模块.域.业务编码}，如 {@code user.validate.nameRequired}。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public final class MessageCodeUtils {

    /**
     * 编码分隔符。
     */
    public static final char SEPARATOR = '.';

    private MessageCodeUtils() {
    }

    /**
     * 拼接编码。
     *
     * @param segments 分段，不可为 {@code null}
     * @return 拼接后的编码
     */
    public static String join(String... segments) {
        return String.join(String.valueOf(SEPARATOR), segments);
    }

    /**
     * 加前缀（如模块名）。
     *
     * @param prefix 前缀，可为 {@code null}（视为空）
     * @param code   编码，不可为 {@code null}
     * @return 带前缀的编码；code 已含前缀时原样返回
     */
    public static String withPrefix(@Nullable String prefix, String code) {
        if (prefix == null || prefix.isEmpty() || code.startsWith(prefix + SEPARATOR)) {
            return code;
        }
        return prefix + SEPARATOR + code;
    }

    /**
     * 规范化编码：将下划线/连字符统一为点分隔，去空白。
     *
     * @param code 原始编码，可为 {@code null}
     * @return 规范化后的编码；{@code null} 输入返回 {@code null}
     */
    public static @Nullable String normalize(@Nullable String code) {
        if (code == null) {
            return null;
        }
        return code.trim().replace('_', SEPARATOR).replace('-', SEPARATOR);
    }

    /**
     * 编码是否含空白/空串等非法形态。
     *
     * @param code 编码，可为 {@code null}
     * @return 非法返回 {@code true}
     */
    public static boolean isInvalid(@Nullable String code) {
        return code == null || code.isBlank();
    }
}
