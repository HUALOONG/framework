package cn.jowen.framework.plugin.hotswap;

import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginContext;
import cn.jowen.framework.plugin.api.PluginManager;
import cn.jowen.framework.plugin.context.DefaultPluginContext;
import cn.jowen.framework.plugin.context.MapPluginConfiguration;
import cn.jowen.framework.plugin.context.SharedData;
import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import cn.jowen.framework.plugin.loader.ClassLoadingStrategy;
import cn.jowen.framework.plugin.loader.PluginLoader;
import org.jspecify.annotations.NullMarked;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 重启策略：文件变更时先卸载旧版本，再经 {@link PluginLoader} 加载新 jar，初始化并启动插件。
 *
 * <p>类加载器由本策略持有（随插件生命周期存活），卸载时释放旧类加载器，关闭时统一清理。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class RestartHotSwapStrategy implements PluginFileWatcher.FileChangeHandler, Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestartHotSwapStrategy.class);

    private final PluginManager manager;
    private final Path pluginsDir;
    private final ClassLoader applicationClassLoader;
    private final Map<String, PluginLoader> loaders = new ConcurrentHashMap<>();
    private final Map<String, String> jarFileByPluginId = new ConcurrentHashMap<>();

    /**
     * 构造重启策略。
     *
     * @param manager   插件管理器，不可为 {@code null}
     * @param pluginsDir 插件目录，不可为 {@code null}
     */
    public RestartHotSwapStrategy(PluginManager manager, Path pluginsDir) {
        this.manager = manager;
        this.pluginsDir = pluginsDir;
        this.applicationClassLoader = Thread.currentThread().getContextClassLoader();
    }

    /**
     * 兼容入口：视为文件修改。
     *
     * @param fileName 文件名，不可为 {@code null}
     */
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
        unloadPluginByJar(fileName);
    }

    private void loadPlugin(String fileName) {
        Path jarPath = pluginsDir.resolve(fileName);
        if (!Files.isRegularFile(jarPath)) {
            return;
        }
        try {
            PluginLoader loader = new PluginLoader(jarPath, applicationClassLoader,
                    ClassLoadingStrategy.FRAMEWORK_API_DELEGATE);
            PluginDescriptor desc = loader.descriptor();
            String id = desc.pluginId();
            if (manager.getPlugin(id) != null) {
                manager.unloadPlugin(id);
            }
            PluginLoader oldLoader = loaders.remove(id);
            if (oldLoader != null) {
                oldLoader.close();
            }
            Plugin plugin = loader.load();
            PluginContext context = new DefaultPluginContext(id, desc, new MapPluginConfiguration(),
                    new SharedData(), manager, applicationClassLoader, loader.getClassLoader(), null);
            manager.initialize(id, plugin, context);
            loaders.put(id, loader);
            jarFileByPluginId.put(id, fileName);
            manager.startPlugin(id);
            LOGGER.info("插件热部署加载完成：" + id);
        } catch (Exception e) {
            LOGGER.warn("插件热部署加载失败：" + fileName + "，原因：" + e.getMessage());
        }
    }

    private void unloadPluginByJar(String fileName) {
        for (Map.Entry<String, String> entry : jarFileByPluginId.entrySet()) {
            if (entry.getValue().equals(fileName)) {
                String id = entry.getKey();
                manager.unloadPlugin(id);
                PluginLoader loader = loaders.remove(id);
                if (loader != null) {
                    try {
                        loader.close();
                    } catch (IOException e) {
                        LOGGER.warn("插件类加载器关闭失败：" + id + "，原因：" + e.getMessage());
                    }
                }
                jarFileByPluginId.remove(id);
                LOGGER.info("插件热部署卸载完成：" + id);
                return;
            }
        }
    }

    @Override
    public void close() throws IOException {
        for (PluginLoader loader : loaders.values()) {
            loader.close();
        }
        loaders.clear();
        jarFileByPluginId.clear();
    }
}