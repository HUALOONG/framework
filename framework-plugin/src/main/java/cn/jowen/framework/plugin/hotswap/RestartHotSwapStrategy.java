package cn.jowen.framework.plugin.hotswap;

import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginManager;
import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import cn.jowen.framework.plugin.descriptor.PluginJsonDescriptorParser;
import cn.jowen.framework.plugin.loader.ClassLoadingStrategy;
import cn.jowen.framework.plugin.loader.PluginLoader;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 重启策略：停止旧插件实例后重新加载同名 jar。
 *
 * @author 王飞
 */
@NullMarked
public final class RestartHotSwapStrategy implements PluginFileWatcher.FileChangeHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestartHotSwapStrategy.class);

    private final PluginManager manager;
    private final Path pluginsDir;

    public RestartHotSwapStrategy(PluginManager manager, Path pluginsDir) {
        this.manager = manager;
        this.pluginsDir = pluginsDir;
    }

    public void onPluginChange(String fileName) {
        onFileModified(fileName);
    }

    @Override
    public void onFileCreated(String fileName) {
        loadPlugin(fileName);
    }

    @Override
    public void onFileModified(String fileName) {
        loadPlugin(fileName);
    }

    @Override
    public void onFileDeleted(String fileName) {
        // 从文件名提取插件 id 并卸载
        String pluginId = extractPluginId(fileName);
        if (pluginId != null) {
            try {
                manager.unloadPlugin(pluginId);
            } catch (Exception ignored) {
            }
        }
    }

    private void loadPlugin(String fileName) {
        Path jarPath = pluginsDir.resolve(fileName);
        if (!Files.isRegularFile(jarPath)) return;
        try {
            PluginDescriptor desc = new PluginJsonDescriptorParser().load(jarPath);
            String id = desc.pluginId();
            // 先卸载旧版本
            Plugin existing = manager.getPlugin(id);
            if (existing != null) {
                try {
                    manager.unloadPlugin(id);
                } catch (Exception ignored) {
                }
            }
            // 加载新版本
            try (PluginLoader loader = new PluginLoader(jarPath,
                    Thread.currentThread().getContextClassLoader(),
                    ClassLoadingStrategy.FRAMEWORK_API_DELEGATE)) {
                Plugin plugin = loader.load();
                // 注意：实际注册由 PluginManager 完成
            }
        } catch (Exception e) {
            LOGGER.warn("插件热部署失败：" + fileName + "，原因：" + e.getMessage());
        }
    }

    private String extractPluginId(String fileName) {
        return fileName.endsWith(".jar") ? fileName.substring(0, fileName.length() - 4) : fileName;
    }
}
