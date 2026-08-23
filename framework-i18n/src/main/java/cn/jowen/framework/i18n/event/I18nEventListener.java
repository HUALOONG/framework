package cn.jowen.framework.i18n.event;

import cn.jowen.framework.core.event.EventListener;
import org.jspecify.annotations.NullMarked;

/**
 * 国际化事件监听器。实现并注册到 {@link cn.jowen.framework.core.event.EventBus}
 * 即可监听资源重载/加载失败/语言切换等事件。
 *
 * @param <E> 监听的事件类型
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public interface I18nEventListener<E extends I18nEvent> extends EventListener<E> {
}
