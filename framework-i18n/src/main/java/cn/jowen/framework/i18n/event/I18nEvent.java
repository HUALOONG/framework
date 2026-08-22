package cn.jowen.framework.i18n.event;

import cn.jowen.framework.core.event.FrameworkEvent;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 国际化事件基类。所有 i18n 相关事件（资源重载、加载失败、语言切换）应继承此类，
 * 通过 {@link cn.jowen.framework.core.event.EventBus} 发布与订阅。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public abstract class I18nEvent extends FrameworkEvent {

    protected I18nEvent() {
        super();
    }

    protected I18nEvent(@Nullable Object source) {
        super(source);
    }
}
