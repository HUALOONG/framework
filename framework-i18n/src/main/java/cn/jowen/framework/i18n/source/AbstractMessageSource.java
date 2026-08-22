package cn.jowen.framework.i18n.source;

import cn.jowen.framework.i18n.api.MessageSource;
import cn.jowen.framework.i18n.api.ReloadableMessageSource;
import cn.jowen.framework.i18n.format.FormatterRegistry;
import cn.jowen.framework.i18n.format.MessageFormatter;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 消息源抽象基类：统一解析流程（按区域精确 → 语言 → 默认回退）与参数化格式化，
 * 子类只需实现 {@link #loadRaw(String, Locale)} 提供原始文案。
 *
 * <p>解析策略：
 * <ol>
 *   <li>按请求区域（语言+国家）精确查找；</li>
 *   <li>按语言级别查找；</li>
 *   <li>按默认区域（无后缀）查找。</li>
 * </ol>
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public abstract class AbstractMessageSource implements MessageSource, ReloadableMessageSource {

    /** 默认格式化器注册表。 */
    protected final FormatterRegistry formatterRegistry = new FormatterRegistry();

    private @Nullable MessageFormatter formatter;

    @Override
    public @Nullable String getMessage(String code, Locale locale, @Nullable Object @Nullable [] args) {
        String raw = resolveRaw(code, locale);
        if (raw == null) {
            return null;
        }
        return currentFormatter().format(raw, args, locale);
    }

    @Override
    public boolean contains(String code, Locale locale) {
        return resolveRaw(code, locale) != null;
    }

    @Override
    public void reload() {
        // 默认无缓存可刷新；子类若带缓存可覆写
    }

    /**
     * 按区域回退链解析原始文案。
     *
     * @param code   消息键
     * @param locale 区域
     * @return 原始文案；未找到返回 {@code null}
     */
    private @Nullable String resolveRaw(String code, Locale locale) {
        String raw = loadRaw(code, locale);
        if (raw != null) {
            return raw;
        }
        if (!locale.getCountry().isEmpty()) {
            raw = loadRaw(code, new Locale(locale.getLanguage()));
            if (raw != null) {
                return raw;
            }
        }
        return loadRaw(code, Locale.ROOT);
    }

    /**
     * 加载指定区域下的原始文案（不做回退，仅本区域精确匹配）。
     *
     * @param code   消息键
     * @param locale 区域
     * @return 原始文案；本区域未找到返回 {@code null}
     */
    protected abstract @Nullable String loadRaw(String code, Locale locale);

    /**
     * 设置格式化器（默认 {@code java-text}）。
     *
     * @param formatter 格式化器，不可为 {@code null}
     */
    public void setFormatter(MessageFormatter formatter) {
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
    }

    /** 设置格式化器名称（从注册表取）。 */
    public void setFormatter(String name) {
        MessageFormatter found = formatterRegistry.get(name);
        if (found == null) {
            throw new IllegalArgumentException("未注册的格式化器: " + name);
        }
        this.formatter = found;
    }

    private MessageFormatter currentFormatter() {
        MessageFormatter f = formatter;
        if (f != null) {
            return f;
        }
        MessageFormatter defaultFormatter = formatterRegistry.get(FormatterRegistry.DEFAULT_FORMATTER);
        return defaultFormatter != null ? defaultFormatter : cn.jowen.framework.i18n.format.JavaTextMessageFormatter.getInstance();
    }
}
