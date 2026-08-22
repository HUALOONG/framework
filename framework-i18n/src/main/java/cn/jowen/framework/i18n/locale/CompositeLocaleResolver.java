package cn.jowen.framework.i18n.locale;

import cn.jowen.framework.i18n.api.LocaleResolver;
import java.util.List;
import java.util.Locale;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 组合区域解析器：按构造顺序依次尝试子解析器，返回首个非 {@code null} 结果。
 * 典型用法：{@code [ParameterLocaleResolver, CookieLocaleResolver, AcceptHeaderLocaleResolver]}。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class CompositeLocaleResolver implements LocaleResolver {

    private final List<LocaleResolver> resolvers;

    public CompositeLocaleResolver(List<LocaleResolver> resolvers) {
        this.resolvers = List.copyOf(resolvers);
    }

    @Override
    public @Nullable Locale resolve(@Nullable Object context) {
        for (LocaleResolver resolver : resolvers) {
            Locale locale = resolver.resolve(context);
            if (locale != null) {
                return locale;
            }
        }
        return null;
    }
}
