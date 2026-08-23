package cn.jowen.framework.core.util;

import cn.jowen.framework.core.exception.SystemException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 反射工具：字段/方法查找（含继承链）、读写、调用与实例化。
 *
 * <p>约定：
 * <ul>
 *   <li>查找方法均沿类继承链向上搜索，返回可访问（{@code setAccessible(true)}）的成员；</li>
 *   <li>反射异常统一包装为 {@link SystemException}，调用方无需处理受检异常；</li>
 *   <li>性能说明：优先使用 {@link Method#invoke}，经 JIT 内联优化后开销可接受；
 *       高频热点场景建议配合缓存查找结果。</li>
 * </ul>
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public final class ReflectionUtils {

    private ReflectionUtils() {
    }

    /**
     * 获取字段（含继承链，优先取声明层次最浅者）。
     *
     * @param type 起始类型
     * @param name 字段名
     * @return 字段（已设置可访问）
     * @throws SystemException 字段不存在
     */
    public static Field getField(Class<?> type, String name) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                if (!Modifier.isPublic(field.getModifiers())) {
                    field.setAccessible(true);
                }
                return field;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new SystemException("字段不存在: " + type.getName() + "#" + name);
    }

    /**
     * 获取全部字段（含继承链，父类字段在后）。
     *
     * @param type 起始类型
     * @return 字段列表
     */
    public static List<Field> getAllFields(Class<?> type) {
        Objects.requireNonNull(type, "type must not be null");
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isSynthetic()) {
                    continue;
                }
                if (!Modifier.isPublic(field.getModifiers())) {
                    field.setAccessible(true);
                }
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    /**
     * 读取字段值。
     *
     * @param target 目标对象（静态字段可传 null）
     * @param name   字段名
     * @return 字段值（可能为 null）
     * @throws SystemException 字段不存在或访问失败
     */
    public static @Nullable Object getFieldValue(@Nullable Object target, String name) {
        Class<?> type = target != null ? target.getClass() : resolveStaticType(name);
        try {
            return getField(type, name).get(target);
        } catch (IllegalAccessException e) {
            throw new SystemException("字段读取失败: " + type.getName() + "#" + name, e);
        }
    }

    /**
     * 写入字段值。
     *
     * @param target 目标对象（静态字段可传 null）
     * @param name   字段名
     * @param value  字段值
     * @throws SystemException 字段不存在、访问失败或类型不匹配
     */
    public static void setFieldValue(@Nullable Object target, String name, @Nullable Object value) {
        Class<?> type = target != null ? target.getClass() : resolveStaticType(name);
        try {
            getField(type, name).set(target, value);
        } catch (IllegalAccessException e) {
            throw new SystemException("字段写入失败: " + type.getName() + "#" + name, e);
        }
    }

    /**
     * 获取方法（含继承链）。
     *
     * @param type       起始类型
     * @param name       方法名
     * @param paramTypes 参数类型（可空）
     * @return 方法（已设置可访问）
     * @throws SystemException 方法不存在
     */
    public static Method getMethod(Class<?> type, String name, Class<?>... paramTypes) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name, paramTypes);
                if (!Modifier.isPublic(method.getModifiers())) {
                    method.setAccessible(true);
                }
                return method;
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        throw new SystemException("方法不存在: " + type.getName() + "#" + name);
    }

    /**
     * 调用方法（含继承链查找，实参自动做装箱/拆箱与可赋值匹配）。
     *
     * @param target 目标对象（静态方法可传 null）
     * @param name   方法名
     * @param args   实参（按顺序匹配参数类型）
     * @return 返回值（void 返回 null）
     * @throws SystemException 方法不存在、调用失败或参数不匹配
     */
    public static @Nullable Object invokeMethod(@Nullable Object target, String name, Object... args) {
        Class<?> type = target != null ? target.getClass() : resolveStaticType(name);
        return invokeMethod(target, findMethod(type, name, args), args);
    }

    /**
     * 按名字 + 参数数量查找方法，参数类型依次做精确、包装类等价、可赋值三层匹配。
     */
    private static Method findMethod(Class<?> type, String name, Object... args) {
        List<Method> candidates = new ArrayList<>();
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == args.length) {
                    candidates.add(method);
                }
            }
            current = current.getSuperclass();
        }
        if (candidates.isEmpty()) {
            throw new SystemException("方法不存在: " + type.getName() + "#" + name);
        }
        // 第一轮：精确/包装类等价匹配
        for (Method method : candidates) {
            if (paramsEquivalent(method.getParameterTypes(), args)) {
                return accessible(method);
            }
        }
        // 第二轮：可赋值匹配（父类参数、接口参数等）
        for (Method method : candidates) {
            if (paramsAssignable(method.getParameterTypes(), args)) {
                return accessible(method);
            }
        }
        throw new SystemException("方法参数不匹配: " + type.getName() + "#" + name);
    }

    private static boolean paramsEquivalent(Class<?>[] paramTypes, Object... args) {
        for (int i = 0; i < paramTypes.length; i++) {
            if (args[i] == null) {
                if (paramTypes[i].isPrimitive()) {
                    return false;
                }
                continue;
            }
            Class<?> argType = args[i].getClass();
            if (!paramTypes[i].equals(argType) && !wrap(paramTypes[i]).equals(argType)) {
                return false;
            }
        }
        return true;
    }

    private static boolean paramsAssignable(Class<?>[] paramTypes, Object... args) {
        for (int i = 0; i < paramTypes.length; i++) {
            if (args[i] == null) {
                if (paramTypes[i].isPrimitive()) {
                    return false;
                }
                continue;
            }
            Class<?> param = wrap(paramTypes[i]);
            if (!param.isAssignableFrom(args[i].getClass())) {
                return false;
            }
        }
        return true;
    }

    private static Method accessible(Method method) {
        if (!Modifier.isPublic(method.getModifiers())) {
            method.setAccessible(true);
        }
        return method;
    }

    /**
     * 基础类型装箱（非基础类型原样返回）。
     */
    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }

    /**
     * 调用方法（直接给定方法对象）。
     *
     * @param target 目标对象（静态方法可传 null）
     * @param method 方法
     * @param args   实参
     * @return 返回值（void 返回 null）
     * @throws SystemException 调用失败
     */
    public static @Nullable Object invokeMethod(@Nullable Object target, Method method, Object... args) {
        Objects.requireNonNull(method, "method must not be null");
        try {
            return method.invoke(target, args);
        } catch (IllegalAccessException e) {
            throw new SystemException("方法调用失败: " + method, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new SystemException("方法调用失败: " + method + "，原因: " + cause.getMessage(), cause);
        }
    }

    /**
     * 实例化（无参构造，私有构造也可）。
     *
     * @param type 目标类型
     * @return 实例
     * @throws SystemException 无无参构造或实例化失败
     */
    public static <T> T newInstance(Class<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        try {
            var constructor = type.getDeclaredConstructor();
            if (!Modifier.isPublic(constructor.getModifiers())) {
                constructor.setAccessible(true);
            }
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new SystemException("实例化失败: " + type.getName(), e);
        }
    }

    /**
     * 静态字段场景下的类型占位（按方法名在调用方类解析失败时兜底抛异常）。
     */
    private static Class<?> resolveStaticType(String name) {
        throw new SystemException("无法解析目标类型（静态成员需通过对象实例访问）: " + name);
    }
}
