package cn.jowen.framework.extras.message.template;

import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于 {@code ${key}} 占位符的轻量模板引擎实现。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class SimpleTemplateEngine implements TemplateEngine {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    @Override
    public Rendered render(Template template, Map<String, Object> variables) {
        return new Rendered(render(template.getTitleTemplate(), variables),
                render(template.getContentTemplate(), variables));
    }

    private String render(String source, Map<String, Object> variables) {
        if (source == null || source.isEmpty()) {
            return "";
        }
        Matcher matcher = PLACEHOLDER.matcher(source);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = variables.get(key);
            String replacement = value == null ? "" : String.valueOf(value);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
