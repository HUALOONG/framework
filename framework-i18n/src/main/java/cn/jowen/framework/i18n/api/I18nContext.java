package cn.jowen.framework.i18n.api;

import cn.jowen.framework.core.context.ContextCarrier;
import cn.jowen.framework.core.context.ContextKey;
import org.jspecify.annotations.NullMarked;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 国际化上下文：当前语言环境的读写与作用域切换，虚拟线程友好。
 *
 * <p>实现说明（冲突修正决议 #3）：Locale 的读写统一委托
 * {@link cn.jowen.framework.core.context.ContextCarrier}（默认 ScopedValue，可配置
 * ThreadLocal 兼容模式），与多租户/数据权限/trace 共享同一载体，跨线程迁移走
 * {@link cn.jowen.framework.core.context.ContextSnapshot}，本类不持有任何上下文字段。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class I18nContext {

    /**
     * 当前语言环境上下文键。
     */
    public static final ContextKey<Locale> LOCALE = ContextKey.named("locale", Locale.class);

    private I18nContext() {
    }

    /**
     * 当前语言环境；未设置时返回 {@link Locale#getDefault()}。
     *
     * @return 当前区域，永不为 {@code null}
     */
    public static Locale getCurrentLocale() {
        Locale locale = ContextCarrier.get(LOCALE);
        return locale != null ? locale : Locale.getDefault();
    }

    /**
     * 设置当前作用域内的语言环境（仅限 {@code runWith} 作用域内；ThreadLocal 兼容模式下任意位置可调用）。
     *
     * @param locale 区域，不可为 {@code null}
     */
    public static void setCurrentLocale(Locale locale) {
        ContextCarrier.set(LOCALE, Objects.requireNonNull(locale, "locale must not be null"));
    }

    /**
     * 当前语言环境；未设置时返回系统默认。
     *
     * @return 当前区域或 {@link Locale#getDefault()}，永不为 {@code null}
     */
    public static Locale getCurrentLocaleOrDefault() {
        return getCurrentLocale();
    }

    /**
     * 在绑定指定语言环境的作用域内执行任务，作用域结束后恢复外部语言环境。
     *
     * @param locale 绑定的区域，不可为 {@code null}
     * @param action 待执行任务，不可为 {@code null}
     * @param <T>    返回值类型
     * @return 任务返回值
     */
    public static <T> T withLocale(Locale locale, Supplier<T> action) {
        Objects.requireNonNull(locale, "locale must not be null");
        Objects.requireNonNull(action, "action must not be null");
        AtomicReference<T> result = new AtomicReference<>();
        ContextCarrier.runWith(LOCALE, locale, () -> result.set(action.get()));
        return result.get();
    }
}
