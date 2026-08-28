package cn.jowen.framework.core.event;

import org.jspecify.annotations.NullMarked;

/**
 * 事件监听器接口。实现类需声明关注的具体事件类型（泛型 {@code E}）。
 *
 * @param <E> 关注的事件类型
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@FunctionalInterface
public interface EventListener<E extends FrameworkEvent> {

    /**
     * 处理事件。
     *
     * @param event 事件，不可为 {@code null}
     */
    void onEvent(E event);
}
