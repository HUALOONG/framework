package cn.jowen.framework.core.event;

import cn.jowen.framework.core.exception.SystemException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 * 进程内事件总线。支持注册监听器并发布事件，可指定 {@link Executor} 实现异步派发。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class EventBus {

    private final Map<Class<?>, List<EventListener<?>>> listeners = new ConcurrentHashMap<>();
    private final @Nullable Executor executor;

    /**
     * 创建同步事件总线（发布线程直接派发）。
     */
    public EventBus() {
        this(null);
    }

    /**
     * 创建事件总线。
     *
     * @param executor 异步派发执行器，为 {@code null} 时同步派发
     */
    public EventBus(@Nullable Executor executor) {
        this.executor = executor;
    }

    private static Class<?> resolveEventType(EventListener<?> listener) {
        Class<?> type = resolveEventType(listener.getClass(), new HashMap<>());
        if (type == null) {
            throw new SystemException("无法推断监听器事件类型：" + listener.getClass().getName());
        }
        return type;
    }

    /**
     * 沿继承链解析事件类型，支持父接口（如 {@code I18nEventListener<ResourceReloadedEvent>}）
     * 与类型变量绑定（泛型继承时父接口的类型参数）。
     *
     * @param type     当前解析的接口
     * @param bindings 已收集的类型变量 → 实际类型 绑定
     * @return 事件类型；无法解析时返回 null
     */
    private static @Nullable Class<?> resolveEventType(Class<?> type, Map<TypeVariable<?>, Type> bindings) {
        for (Type face : type.getGenericInterfaces()) {
            Map<TypeVariable<?>, Type> next = new HashMap<>(bindings);
            Class<?> rawClass;
            Type[] actualArgs;
            if (face instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> raw) {
                rawClass = raw;
                actualArgs = pt.getActualTypeArguments();
            } else if (face instanceof Class<?> raw) {
                rawClass = raw;
                actualArgs = new Type[0];
            } else {
                continue;
            }
            if (rawClass == EventListener.class) {
                return actualArgs.length > 0 ? resolveType(actualArgs[0], next) : null;
            }
            // 记录父接口类型变量 → 实际类型参数 的绑定，供其内部继承链解析
            TypeVariable<?>[] vars = rawClass.getTypeParameters();
            for (int i = 0; i < vars.length && i < actualArgs.length; i++) {
                next.put(vars[i], actualArgs[i]);
            }
            Class<?> nested = resolveEventType(rawClass, next);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    /**
     * 解析类型，支持类型变量绑定。
     *
     * @param type     待解析的类型
     * @param bindings 类型变量 → 实际类型 的绑定
     * @return 解析后的类型；无法解析时返回 null
     */
    private static @Nullable Class<?> resolveType(Type type, Map<TypeVariable<?>, Type> bindings) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        if (type instanceof TypeVariable<?> tv) {
            Type resolved = bindings.get(tv);
            return resolved != null ? resolveType(resolved, bindings) : null;
        }
        return null;
    }

    /**
     * 注册监听器，监听其泛型声明的事件类型。
     *
     * @param listener 监听器，不可为 {@code null}
     * @param <E>      事件类型
     */
    public <E extends FrameworkEvent> void register(EventListener<E> listener) {
        Class<?> eventType = resolveEventType(listener);
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * 发布事件，派发给所有关注的监听器。
     *
     * @param event 事件，不可为 {@code null}
     */
    @SuppressWarnings("unchecked")
    public void publish(FrameworkEvent event) {
        Class<?> type = event.getClass();
        for (Map.Entry<Class<?>, List<EventListener<?>>> entry : listeners.entrySet()) {
            if (entry.getKey().isAssignableFrom(type)) {
                for (EventListener<?> l : entry.getValue()) {
                    dispatch((EventListener<FrameworkEvent>) l, event);
                }
            }
        }
    }

    /**
     * 派发事件给监听器。
     *
     * @param listener 监听器
     * @param event    事件
     */
    private void dispatch(EventListener<FrameworkEvent> listener, FrameworkEvent event) {
        if (executor != null) {
            executor.execute(() -> safeInvoke(listener, event));
        } else {
            safeInvoke(listener, event);
        }
    }

    /**
     * 安全执行监听器。
     *
     * @param listener 监听器
     * @param event    事件
     */
    private void safeInvoke(EventListener<FrameworkEvent> listener, FrameworkEvent event) {
        try {
            listener.onEvent(event);
        } catch (RuntimeException ex) {
            throw new SystemException("事件监听器执行失败：" + listener.getClass().getName(), ex);
        }
    }
}
