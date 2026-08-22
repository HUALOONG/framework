package cn.jowen.framework.i18n.format;

import cn.jowen.framework.i18n.api.FormatException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 命名参数格式化器，占位符语法 {@code {name}} / {@code {age}}，参数以 {@link Map} 形式传入。
 * 未提供的参数名按空字符串处理；模板含非法占位符（未闭合的 {@code {}}）时抛
 * {@link FormatException}。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class NamedParameterMessageFormatter implements MessageFormatter {

    public static final String NAME = "named-parameter";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)}");

    private static final NamedParameterMessageFormatter INSTANCE = new NamedParameterMessageFormatter();

    private NamedParameterMessageFormatter() {
    }

    /** 单例实例。 */
    public static NamedParameterMessageFormatter getInstance() {
        return INSTANCE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String format(String pattern, @Nullable Object @Nullable [] args, Locale locale) {
        Map<String, Object> params;
        if (args == null || args.length == 0) {
            params = Map.of();
        } else if (args.length == 1 && args[0] instanceof Map<?, ?> map) {
            params = (Map<String, Object>) map;
        } else {
            throw new FormatException("命名参数格式化要求单个 Map 参数: " + pattern);
        }
        if (pattern.indexOf('{') >= 0 && pattern.indexOf('}') < 0) {
            throw new FormatException("模板存在未闭合的占位符: " + pattern);
        }
        Matcher matcher = PLACEHOLDER.matcher(pattern);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            Object value = params.get(name);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value != null ? String.valueOf(value) : ""));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @Override
    public String name() {
        return NAME;
    }
}
