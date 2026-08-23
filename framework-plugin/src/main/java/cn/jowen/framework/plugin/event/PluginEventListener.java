package cn.jowen.framework.plugin.event;

import org.jspecify.annotations.NullMarked;

/**
 * 插件事件监听器接口。
 *
 * @author 王飞
 */
@NullMarked
public interface PluginEventListener {

    /**
     * 处理插件事件。
     *
     * @param event 事件，不可为 {@code null}
     */
    void onEvent(PluginEvent event);
}
