package cn.jowen.framework.i18n.format;

import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

/**
 * 格式化器注册表。按名称管理 {@link MessageFormatter}，预置
 * {@link JavaTextMessageFormatter}（默认）与 {@link NamedParameterMessageFormatter}，
 * 支持按需覆盖与追加（如启用 {@link IcuMessageFormatter}）。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class FormatterRegistry {

    /** 默认格式化器名称。 */
    public static final String DEFAULT_FORMATTER = JavaTextMessageFormatter.NAME;

    private final Map<String, MessageFormatter> formatters = new LinkedHashMap<>();

    public FormatterRegistry() {
        register(JavaTextMessageFormatter.getInstance());
        register(NamedParameterMessageFormatter.getInstance());
    }

    /**
     * 注册格式化器，同名列覆盖旧实现。
     *
     * @param formatter 格式化器，不可为 {@code null}
     * @return 自身（链式调用）
     */
    public FormatterRegistry register(MessageFormatter formatter) {
        formatters.put(formatter.name(), formatter);
        return this;
    }

    /**
     * 按名称获取格式化器。
     *
     * @param name 名称，不可为 {@code null}
     * @return 格式化器；未注册返回 {@code null}
     */
    public @org.jspecify.annotations.Nullable MessageFormatter get(String name) {
        return formatters.get(name);
    }

    /** 是否包含指定名称。 */
    public boolean contains(String name) {
        return formatters.containsKey(name);
    }

    /** 已注册的格式化器名称集合（按注册顺序）。 */
    public Map<String, MessageFormatter> all() {
        return Map.copyOf(formatters);
    }
}
