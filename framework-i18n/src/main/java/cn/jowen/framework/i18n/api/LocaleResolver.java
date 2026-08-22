package cn.jowen.framework.i18n.api;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * 区域解析器。从任意上下文（请求头、参数、线程本地）解析目标区域，不依赖 Servlet。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public interface LocaleResolver {

    /**
     * 解析区域。
     *
     * @param context 解析上下文（如请求对象），可为 {@code null}
     * @return 区域；无法解析返回 {@code null}
     */
    @Nullable Locale resolve(@Nullable Object context);
}
