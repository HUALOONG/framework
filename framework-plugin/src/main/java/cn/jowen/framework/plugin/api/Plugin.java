package cn.jowen.framework.plugin.api;

import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import cn.jowen.framework.plugin.registry.Extension;
import cn.jowen.framework.plugin.registry.ExtensionPoint;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * 插件核心接口。所有插件实现必须满足此契约。
 *
 * @author 王飞
 */
@NullMarked
public interface Plugin {

    /**
     * 启动插件，注入运行时上下文。
     *
     * @param context 插件上下文，不可为 {@code null}
     */
    void start(PluginContext context);

    /**
     * 停止插件，释放资源。
     */
    void stop();

    /**
     * 返回插件描述符。
     *
     * @return 插件描述符，不可为 {@code null}
     */
    PluginDescriptor getDescriptor();

    /**
     * 返回当前插件状态。
     *
     * @return 当前状态，不可为 {@code null}
     */
    PluginState getState();

    /**
     * 返回本插件提供的扩展实现列表。
     *
     * @return 扩展列表，不可为 {@code null}（可能为空）
     */
    default List<Extension> getExtensions() {
        return List.of();
    }

    /**
     * 返回本插件声明的扩展点列表。
     *
     * @return 扩展点列表，不可为 {@code null}（可能为空）
     */
    default List<ExtensionPoint> getExtensionPoints() {
        return List.of();
    }
}
