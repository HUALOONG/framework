package cn.jowen.framework.plugin.loader;

import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 插件加载信息聚合。
 *
 * @param descriptor  插件描述符
 * @param classLoader 插件类加载器
 * @param instance    插件实例
 * @param pluginPath  插件 jar 路径
 * @param loadedAt    加载时间
 * @author 王飞
 */
@NullMarked
public record PluginInfo(
        PluginDescriptor descriptor,
        PluginClassLoader classLoader,
        @Nullable Plugin instance,
        java.nio.file.Path pluginPath,
        Instant loadedAt
) implements AutoCloseable {

    @Override
    public void close() throws java.io.IOException {
        if (classLoader != null) {
            classLoader.close();
        }
    }
}
