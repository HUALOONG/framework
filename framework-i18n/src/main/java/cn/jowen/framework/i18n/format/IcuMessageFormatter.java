package cn.jowen.framework.i18n.format;

import cn.jowen.framework.i18n.api.FormatException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * ICU4J {@code com.ibm.icu.text.MessageFormat} 格式化器，占位符语法
 * {@code {0, choice, ...}} / {@code {name}} 等 ICU 风格，支持更完善的选择与复数规则。
 *
 * <p>ICU4J 为可选依赖：通过反射桥接调用，classpath 未引入
 * {@code com.ibm.icu:icu4j} 时 {@link #format} 抛 {@link FormatException} 并提示引入坐标。
 * 已引入时自动使用 ICU 能力，无需改动调用方。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public final class IcuMessageFormatter implements MessageFormatter {

    public static final String NAME = "icu";

    private static final String ICU_FORMAT_CLASS = "com.ibm.icu.text.MessageFormat";

    private static final @Nullable Class<?> ICU_TYPE = resolveIcuType();

    private static final IcuMessageFormatter INSTANCE = new IcuMessageFormatter();

    private IcuMessageFormatter() {
    }

    /**
     * 单例实例。
     */
    public static IcuMessageFormatter getInstance() {
        return INSTANCE;
    }

    /**
     * ICU4J 是否在 classpath 中。
     */
    public static boolean available() {
        return ICU_TYPE != null;
    }

    private static @Nullable Class<?> resolveIcuType() {
        try {
            return Class.forName(ICU_FORMAT_CLASS, false, IcuMessageFormatter.class.getClassLoader());
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    @Override
    public String format(String pattern, @Nullable Object @Nullable [] args, Locale locale) {
        if (ICU_TYPE == null) {
            throw new FormatException("ICU4J 未引入 classpath，请添加 com.ibm.icu:icu4j 依赖以启用 ICU 格式化");
        }
        try {
            Constructor<?> ctor = ICU_TYPE.getConstructor(String.class, Locale.class);
            Object icuFormat = ctor.newInstance(pattern, locale);
            Method formatMethod = ICU_TYPE.getMethod("format", Object.class);
            Object[] payload = args != null ? args : new Object[0];
            return (String) formatMethod.invoke(icuFormat, (Object) payload);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException
                 | InvocationTargetException ex) {
            throw new FormatException("ICU 消息格式化失败: " + pattern, ex);
        }
    }

    @Override
    public String name() {
        return NAME;
    }
}
