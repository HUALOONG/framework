package cn.jowen.framework.i18n.support;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 占位符解析器：替换模板中的 {@code ${name}} 占位符为参数值，未提供的键替换为空串。
 * 与格式化器区分：本工具面向配置模板（如邮件/通知文案），不参与消息参数化。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class PlaceholderResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    private static final PlaceholderResolver INSTANCE = new PlaceholderResolver();

    private PlaceholderResolver() {
    }

    /** 单例实例。 */
    public static PlaceholderResolver getInstance() {
        return INSTANCE;
    }

    /**
     * 解析模板。
     *
     * @param template 模板，可为 {@code null}
     * @param values   参数表，不可为 {@code null}
     * @return 解析结果；模板为 {@code null} 返回 {@code null}
     */
    public @Nullable String resolve(@Nullable String template, Map<String, Object> values) {
        if (template == null) {
            return null;
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            Object value = values.get(matcher.group(1));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value != null ? String.valueOf(value) : ""));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /** 模板是否包含占位符。 */
    public boolean hasPlaceholders(@Nullable String template) {
        return template != null && PLACEHOLDER.matcher(template).find();
    }
}
