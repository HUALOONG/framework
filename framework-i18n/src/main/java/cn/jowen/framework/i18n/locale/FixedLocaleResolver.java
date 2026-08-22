package cn.jowen.framework.i18n.locale;

import cn.jowen.framework.i18n.api.LocaleResolver;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 固定区域解析器：始终返回构造时指定的区域，忽略上下文。适用于单一语言环境场景
 * （如纯中文内网系统）。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class FixedLocaleResolver implements LocaleResolver {

    private final Locale locale;

    public FixedLocaleResolver(Locale locale) {
        this.locale = Objects.requireNonNull(locale, "locale must not be null");
    }

    @Override
    public @Nullable Locale resolve(@Nullable Object context) {
        return locale;
    }
}
