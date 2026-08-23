package cn.jowen.framework.i18n.format;

import cn.jowen.framework.i18n.api.FormatException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.text.MessageFormat;
import java.util.Locale;

/**
 * 基于 JDK {@link MessageFormat} 的格式化器（默认实现），占位符语法 {@code {0} {1}}，
 * 支持选择/复数/日期等高级模式。线程安全：内部以每次调用新建 {@link MessageFormat} 实例避免
 * {@link MessageFormat} 非线程安全带来的串扰。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public final class JavaTextMessageFormatter implements MessageFormatter {

    public static final String NAME = "java-text";

    private static final JavaTextMessageFormatter INSTANCE = new JavaTextMessageFormatter();

    private JavaTextMessageFormatter() {
    }

    /**
     * 单例实例。
     */
    public static JavaTextMessageFormatter getInstance() {
        return INSTANCE;
    }

    @Override
    public String format(String pattern, @Nullable Object @Nullable [] args, Locale locale) {
        try {
            return new MessageFormat(pattern, locale).format(args != null ? args : new Object[0]);
        } catch (IllegalArgumentException ex) {
            throw new FormatException("消息格式化失败: " + pattern, ex);
        }
    }

    @Override
    public String name() {
        return NAME;
    }
}
