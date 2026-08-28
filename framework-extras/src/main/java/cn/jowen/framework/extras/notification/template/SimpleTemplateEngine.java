package cn.jowen.framework.extras.notification.template;

import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简单模板引擎：零依赖实现 {@link NotificationTemplateEngine}，用 {@code ${key}} 占位符替换。
 *
 * <p>从 {@code params}（Map）取值替换；未命中的占位符保留原样，避免吞掉变量名。
 * 适用于邮件/短信/钉钉/企微等渠道的轻量消息渲染，无需引入模板引擎依赖。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
public final class SimpleTemplateEngine implements NotificationTemplateEngine {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    @Override
    public String render(String template, Object params) {
        if (template == null) {
            throw new IllegalArgumentException("template must not be null");
        }
        if (params == null) {
            throw new IllegalArgumentException("params must not be null");
        }
        Map<?, ?> values = params instanceof Map<?, ?> map ? map : Map.of();
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            Object value = values.get(matcher.group(1));
            // 未命中保留占位符原文；命中需转义 $ 与 \，否则 appendReplacement 会误替换
            matcher.appendReplacement(sb, value == null
                    ? Matcher.quoteReplacement(matcher.group())
                    : Matcher.quoteReplacement(String.valueOf(value)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}