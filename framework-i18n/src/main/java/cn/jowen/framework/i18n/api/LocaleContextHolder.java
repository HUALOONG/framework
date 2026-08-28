package cn.jowen.framework.i18n.api;

import cn.jowen.framework.core.context.ContextCarrier;
import org.jspecify.annotations.NullMarked;

import java.util.Locale;

/**
 * 区域上下文持有者。读路径统一委托 {@link ContextCarrier}（与 {@link I18nContext} 共享载体），
 * 写路径保留 ThreadLocal 以兼容作用域外调用（ScopedValue 模式下 set 仅在 {@code runWith} 作用域内可用）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class LocaleContextHolder {

    private static final ThreadLocal<Locale> HOLDER = new ThreadLocal<>();

    private LocaleContextHolder() {
    }

    /**
     * @return 当前区域：优先取 {@link I18nContext}（ContextCarrier 作用域内）的值，
     *         其次取本类 ThreadLocal，均未设置时返回 {@link Locale#getDefault()}
     */
    public static Locale getLocale() {
        Locale locale = ContextCarrier.get(I18nContext.LOCALE);
        if (locale == null) {
            locale = HOLDER.get();
        }
        return locale != null ? locale : Locale.getDefault();
    }

    /**
     * 设置当前线程区域（写 ThreadLocal；兼容作用域外调用）。
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
