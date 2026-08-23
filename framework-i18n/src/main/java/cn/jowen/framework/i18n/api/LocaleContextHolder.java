package cn.jowen.framework.i18n.api;

import org.jspecify.annotations.NullMarked;

import java.util.Locale;

/**
 * 区域上下文持有者。基于线程本地，避免对 Servlet {@code RequestContextHolder} 的耦合。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public final class LocaleContextHolder {

    private static final ThreadLocal<Locale> HOLDER = new ThreadLocal<>();

    private LocaleContextHolder() {
    }

    /**
     * @return 当前线程区域；未设置返回 {@link Locale#getDefault()}
     */
    public static Locale getLocale() {
        Locale locale = HOLDER.get();
        return locale != null ? locale : Locale.getDefault();
    }

    /**
     * 设置当前线程区域。
     *
     * @param locale 区域，不可为 {@code null}
     */
    public static void setLocale(Locale locale) {
        HOLDER.set(locale);
    }

    /**
     * 清除当前线程区域（建议在请求结束时调用，避免泄漏）。
     */
    public static void resetLocale() {
        HOLDER.remove();
    }
}
