package cn.jowen.framework.i18n.source;

import cn.jowen.framework.i18n.api.MessageSource;
import cn.jowen.framework.i18n.api.ReloadableMessageSource;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 聚合消息源。按注册顺序委托多个子源，命中即返回；支持热加载全部子源。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class CompositeMessageSource implements ReloadableMessageSource {

    private final List<MessageSource> sources = new ArrayList<>();

    /** 追加子源（顺序即优先级）。 */
    public void add(MessageSource source) {
        sources.add(source);
    }

    @Override
    public @Nullable String getMessage(String code, Locale locale, @Nullable Object @Nullable [] args) {
        for (MessageSource source : sources) {
            String msg = source.getMessage(code, locale, args);
            if (msg != null) {
                return msg;
            }
        }
        return null;
    }

    @Override
    public boolean contains(String code, Locale locale) {
        for (MessageSource source : sources) {
            if (source.contains(code, locale)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void reload() {
        for (MessageSource source : sources) {
            if (source instanceof ReloadableMessageSource reloadable) {
                reloadable.reload();
            }
        }
    }

    /**
     * 便捷方法：格式化消息。
     *
     * @param pattern 消息模板
     * @param args    参数
     * @param locale  区域
     * @return 格式化结果
     */
    static String format(String pattern, @Nullable Object @Nullable [] args, Locale locale) {
        if (args == null || args.length == 0) {
            return pattern;
        }
        return new MessageFormat(pattern, locale).format(args);
    }
}
