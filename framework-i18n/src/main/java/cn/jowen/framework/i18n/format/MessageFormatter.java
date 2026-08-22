package cn.jowen.framework.i18n.format;

import java.util.Locale;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 消息格式化器。将带占位符的消息模板与参数数组结合产出最终文案，与具体消息源解耦，
 * 通过 {@link FormatterRegistry} 按名称注册与获取。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public interface MessageFormatter {

    /**
     * 格式化消息。
     *
     * @param pattern 消息模板（含占位符），不可为 {@code null}
     * @param args    格式化参数，可为 {@code null}（视为空数组）
     * @param locale  区域，不可为 {@code null}
     * @return 格式化后的消息
     * @throws cn.jowen.framework.i18n.FormatException 模板语法错误或参数不匹配时抛出
     */
    String format(String pattern, @Nullable Object @Nullable [] args, Locale locale);

    /**
     * 格式化器名称（注册键），如 {@code java-text} / {@code named-parameter} / {@code icu}。
     *
     * @return 名称，不可为 {@code null}
     */
    String name();
}
