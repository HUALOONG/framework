package cn.jowen.framework.core.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * 类与类加载相关工具。优先使用线程上下文类加载器，回退到当前类加载器。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public final class ClassUtils {

    private ClassUtils() {
    }

    /**
     * 获取合适的类加载器：优先 {@code Thread.getContextClassLoader()}，为空则回退到本类加载器。
     *
     * @return 类加载器，不可为 {@code null}
     */
    public static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ClassUtils.class.getClassLoader();
        }
        return Objects.requireNonNullElse(cl, ClassLoader.getSystemClassLoader());
    }

    /**
     * 按全限定名加载类，调用方需处理 {@link ClassNotFoundException}。
     *
     * @param className 类全限定名，不可为 {@code null}
     * @return 加载到的类，不可为 {@code null}
     * @throws ClassNotFoundException 类不存在时抛出
     */
    public static Class<?> forName(String className) throws ClassNotFoundException {
        Objects.requireNonNull(className, "className");
        return Class.forName(className, true, getDefaultClassLoader());
    }

    /**
     * 判断两个对象的类型是否相同（兼容 {@code null}）。
     *
     * @param a 对象 A，可为 {@code null}
     * @param b 对象 B，可为 {@code null}
     * @return 类型一致返回 {@code true}
     */
    public static boolean isSameType(@Nullable Object a, @Nullable Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.getClass() == b.getClass();
    }
}
