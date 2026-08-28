package cn.jowen.framework.i18n.locale;

import cn.jowen.framework.i18n.api.LocaleResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Accept-Language 头区域解析器：按 RFC 7231 解析请求头，质量值降序取首个；
 * 可配置支持的区域集合用于就近匹配。上下文须为 {@link HttpServletRequest}。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class AcceptHeaderLocaleResolver implements LocaleResolver {

    private final @Nullable List<Locale> supportedLocales;

    public AcceptHeaderLocaleResolver() {
        this(null);
    }

    /**
     * 构造解析器。
     *
     * @param supportedLocales 支持的区域集合；为 {@code null} 时不约束（直接返回头中首选区域）
     */
    public AcceptHeaderLocaleResolver(@Nullable List<Locale> supportedLocales) {
        this.supportedLocales = supportedLocales != null ? List.copyOf(supportedLocales) : null;
    }

    @Override
    public @Nullable Locale resolve(@Nullable Object context) {
        if (!(context instanceof HttpServletRequest request)) {
            return null;
        }
        List<Locale> parsed = LocaleUtils.parseAcceptLanguage(request.getHeader("Accept-Language"));
        if (parsed.isEmpty()) {
            return null;
        }
        if (supportedLocales == null) {
            return parsed.getFirst();
        }
        return LocaleUtils.match(parsed.getFirst(), supportedLocales);
    }
}
