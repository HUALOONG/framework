package cn.jowen.framework.core.context;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * {@code java.lang.ScopedValue} 反射桥接（内部实现，不对外公开）。
 *
 * <p>背景：{@code ScopedValue} 于 JDK 21 为预览 API（JEP 429/446），JDK 22 起正式化（JEP 464）。
 * 若在源码中直接引用 {@code ScopedValue} 类型，类文件将被标记为 preview，导致所有使用方
 * 被迫携带 {@code --enable-preview} 运行——这对框架库不可接受。
 *
 * <p>策略：编译期零类型引用，运行时经反射探测并调用：
 * <ul>
 *   <li>JDK 22+：正式 API 可用，ScopedValue 模式生效；</li>
 *   <li>JDK 21（未启用预览）：实例化被 JVM 拦截，{@link #available()} 返回 false，
 *       {@link ContextCarrier} 自动降级为 ThreadLocal 模式。</li>
 * </ul>
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
final class ScopedValueBridge {

    private static final boolean AVAILABLE;
    private static final @Nullable Method NEW_INSTANCE;
    private static final @Nullable Method IS_BOUND;
    private static final @Nullable Method GET;
    private static final @Nullable Method WHERE;
    private static final @Nullable Method CARRIER_RUN;

    static {
        boolean available = false;
        Method newInstance = null;
        Method isBound = null;
        Method get = null;
        Method where = null;
        Method carrierRun = null;
        try {
            Class<?> scopedValue = Class.forName("java.lang.ScopedValue");
            newInstance = scopedValue.getMethod("newInstance");
            where = scopedValue.getMethod("where", scopedValue, Object.class);
            isBound = scopedValue.getMethod("isBound");
            get = scopedValue.getMethod("get");
            Class<?> carrier = Class.forName("java.lang.ScopedValue$Carrier");
            carrierRun = carrier.getMethod("run", Runnable.class);
            // 全链路探测：实例化 + 绑定 + 执行空任务，任一环节失败（如 JDK 21 预览未启用）则整体不可用
            Object probe = newInstance.invoke(null);
            Object probeCarrier = where.invoke(null, probe, "probe");
            carrierRun.invoke(probeCarrier, (Runnable) () -> {
            });
            available = true;
        } catch (Throwable ignored) {
            // ScopedValue 不可用（预览未启用等），降级处理
        }
        AVAILABLE = available;
        NEW_INSTANCE = available ? newInstance : null;
        IS_BOUND = available ? isBound : null;
        GET = available ? get : null;
        WHERE = available ? where : null;
        CARRIER_RUN = available ? carrierRun : null;
    }

    private ScopedValueBridge() {
    }

    /**
     * ScopedValue 在当前 JVM 是否可用。
     */
    static boolean available() {
        return AVAILABLE;
    }

    /**
     * 创建 ScopedValue 实例（不可用时返回 null）。
     */
    static @Nullable Object newInstance() {
        return invoke(NEW_INSTANCE, null);
    }

    /**
     * 是否已绑定。
     */
    static boolean isBound(Object scopedValue) {
        Boolean result = invoke(IS_BOUND, scopedValue);
        return result != null && result;
    }

    /**
     * 读取绑定值（未绑定时返回 null）。
     */
    static @Nullable Object get(Object scopedValue) {
        return invoke(GET, scopedValue);
    }

    /**
     * 在绑定值的作用域内执行任务。
     */
    static void run(Object scopedValue, @Nullable Object value, Runnable task) {
        Object carrier = invoke(WHERE, null, scopedValue, value);
        invoke(CARRIER_RUN, carrier, task);
    }

    @SuppressWarnings("unchecked")
    private static <T> @Nullable T invoke(@Nullable Method method, @Nullable Object target, Object... args) {
        if (method == null) {
            return null;
        }
        try {
            return (T) method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new IllegalStateException("ScopedValue 调用失败: " + method.getName(), cause);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("ScopedValue 调用失败: " + method.getName(), e);
        }
    }
}
