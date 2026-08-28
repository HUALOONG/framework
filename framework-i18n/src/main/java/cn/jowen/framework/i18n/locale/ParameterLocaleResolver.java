package cn.jowen.framework.i18n.locale;

import cn.jowen.framework.i18n.api.LocaleResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * 请求参数区域解析器：从查询参数（默认 {@code lang}，如 {@code ?lang=zh-CN}）解析区域。
 * 上下文须为 {@link HttpServletRequest}；参数缺失或非法返回 {@code null}。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
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
