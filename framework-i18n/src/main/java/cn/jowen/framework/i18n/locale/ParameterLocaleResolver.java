package cn.jowen.framework.i18n.locale;

import cn.jowen.framework.i18n.api.LocaleResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 请求参数区域解析器：从查询参数（默认 {@code lang}，如 {@code ?lang=zh-CN}）解析区域。
 * 上下文须为 {@link HttpServletRequest}；参数缺失或非法返回 {@code null}。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class ParameterLocaleResolver implements LocaleResolver {

    public static final String DEFAULT_PARAM = "lang";

    private final String paramName;

    public ParameterLocaleResolver() {
        this(DEFAULT_PARAM);
    }

    /**
     * 构造解析器。
     *
     * @param paramName 参数名，不可为 {@code null}
     */
    public ParameterLocaleResolver(String paramName) {
        this.paramName = paramName != null ? paramName : DEFAULT_PARAM;
    }

    @Override
    public @Nullable Locale resolve(@Nullable Object context) {
        if (!(context instanceof HttpServletRequest request)) {
            return null;
        }
        String value = request.getParameter(paramName);
        return LocaleUtils.parseTag(value);
    }
}
