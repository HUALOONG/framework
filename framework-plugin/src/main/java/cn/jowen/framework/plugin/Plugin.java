package cn.jowen.framework.plugin;

import cn.jowen.framework.core.lifecycle.Lifecycle;
import org.jspecify.annotations.NullMarked;

/**
 * 插件契约。所有插件实现需同时具备生命周期（初始化/销毁）与基础元数据。
 *
 * <p>插件可借助 core 的 {@code spi} 机制（{@code @SPI}/{@code @SPIImplementation}）被发现，
 * 或经 {@link PluginManager} 显式注册。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public interface Plugin extends Lifecycle {

    /** @return 插件唯一标识，不可为 {@code null} */
    String id();

    /** @return 插件版本，不可为 {@code null} */
    String version();

    /** @return 插件描述，不可为 {@code null} */
    default String description() {
        return "";
    }
}
